package com.quizapp.service;

import com.quizapp.dto.PenaltyKickInputDto;
import com.quizapp.dto.PenaltyShootoutAdminDetailDto;
import com.quizapp.dto.PenaltyShootoutRequest;
import com.quizapp.dto.PenaltyShootoutSummaryDto;
import com.quizapp.exception.ResourceNotFoundException;
import com.quizapp.model.Athlete;
import com.quizapp.model.PenaltyCandidate;
import com.quizapp.model.PenaltyKick;
import com.quizapp.model.PenaltyShootout;
import com.quizapp.repository.AthleteRepository;
import com.quizapp.repository.PenaltyShootoutRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Service
public class PenaltyShootoutAdminService {

    private final PenaltyShootoutRepository penaltyShootoutRepository;
    private final AthleteRepository athleteRepository;
    private final AthleteService athleteService;

    public PenaltyShootoutAdminService(PenaltyShootoutRepository penaltyShootoutRepository,
                                        AthleteRepository athleteRepository, AthleteService athleteService) {
        this.penaltyShootoutRepository = penaltyShootoutRepository;
        this.athleteRepository = athleteRepository;
        this.athleteService = athleteService;
    }

    @Transactional(readOnly = true)
    public List<PenaltyShootoutSummaryDto> findAll() {
        return penaltyShootoutRepository.findAll().stream()
                .map(this::toSummaryDto)
                .sorted((a, b) -> {
                    // Newest match first, undated ones (matchDate == null) last.
                    if (a.getMatchDate() == null && b.getMatchDate() == null) return 0;
                    if (a.getMatchDate() == null) return 1;
                    if (b.getMatchDate() == null) return -1;
                    return b.getMatchDate().compareTo(a.getMatchDate());
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PenaltyShootoutAdminDetailDto getOne(Long id) {
        return toDetailDto(findShootout(id));
    }

    @Transactional
    public PenaltyShootoutAdminDetailDto create(PenaltyShootoutRequest request) {
        PenaltyShootout shootout = new PenaltyShootout();
        applyRequest(shootout, request);
        return toDetailDto(penaltyShootoutRepository.save(shootout));
    }

    @Transactional
    public PenaltyShootoutAdminDetailDto update(Long id, PenaltyShootoutRequest request) {
        PenaltyShootout shootout = findShootout(id);
        applyRequest(shootout, request);
        return toDetailDto(penaltyShootoutRepository.save(shootout));
    }

    @Transactional
    public void delete(Long id) {
        if (!penaltyShootoutRepository.existsById(id)) {
            throw new ResourceNotFoundException("No penalty shootout found with id " + id);
        }
        penaltyShootoutRepository.deleteById(id);
    }

    private void applyRequest(PenaltyShootout shootout, PenaltyShootoutRequest request) {
        // Kick order must be a clean 1..N sequence, no gaps or repeats -
        // otherwise "kick 6" could exist with no "kick 5" ever assigned, which
        // would just be confusing on the play board.
        Set<Integer> orders = new TreeSet<>();
        for (PenaltyKickInputDto k : request.getKicks()) {
            if (!orders.add(k.getKickOrder())) {
                throw new IllegalArgumentException("Two kicks can't share order " + k.getKickOrder() + ".");
            }
        }
        int n = request.getKicks().size();
        for (int expected = 1; expected <= n; expected++) {
            if (!orders.contains(expected)) {
                throw new IllegalArgumentException(
                        "Kick order must run 1.." + n + " with no gaps - missing kick " + expected + ".");
            }
        }

        shootout.setUpdatedAt(Instant.now());
        shootout.setTitle(request.getTitle());
        shootout.setCompetition(request.getCompetition());
        shootout.setMatchDate(request.getMatchDate());
        shootout.setTeamName(request.getTeamName());
        shootout.setTeamCrestUrl(request.getTeamCrestUrl());
        shootout.setOpponentName(request.getOpponentName());
        shootout.setOpponentCrestUrl(request.getOpponentCrestUrl());
        shootout.setTeamPensScored(request.getTeamPensScored());
        shootout.setOpponentPensScored(request.getOpponentPensScored());
        shootout.setMaxStrikes(request.getMaxStrikes());

        List<Long> candidateAthleteIds = request.getCandidateAthleteIds().stream()
                .distinct().collect(Collectors.toList());
        List<Athlete> foundAthletes = athleteRepository.findAllById(candidateAthleteIds);
        Map<Long, Athlete> athleteById = foundAthletes.stream()
                .collect(Collectors.toMap(Athlete::getId, a -> a));
        if (athleteById.size() != candidateAthleteIds.size()) {
            List<Long> missing = candidateAthleteIds.stream()
                    .filter(aid -> !athleteById.containsKey(aid))
                    .collect(Collectors.toList());
            throw new IllegalArgumentException("No athlete found with id(s) " + missing);
        }

        // Reuse existing candidate/kick rows for the same athlete rather than
        // deleting and recreating every save - same reasoning as
        // LineupAdminService.applyRequest.
        Map<Long, PenaltyCandidate> existingCandidateByAthleteId = shootout.getCandidates().stream()
                .collect(Collectors.toMap(c -> c.getAthlete().getId(), c -> c, (a, b) -> a));
        Set<PenaltyCandidate> candidates = candidateAthleteIds.stream()
                .map(athleteId -> {
                    PenaltyCandidate c = existingCandidateByAthleteId.getOrDefault(athleteId, new PenaltyCandidate());
                    c.setAthlete(athleteById.get(athleteId));
                    return c;
                })
                .collect(Collectors.toSet());
        shootout.setCandidates(candidates);

        Map<Integer, PenaltyKick> existingByKickOrder = shootout.getKicks().stream()
                .collect(Collectors.toMap(PenaltyKick::getKickOrder, k -> k, (a, b) -> a));

        Set<PenaltyKick> kicks = new HashSet<>();
        for (PenaltyKickInputDto input : request.getKicks()) {
            Athlete athlete = athleteById.get(input.getAthleteId());
            if (athlete == null) {
                athlete = athleteRepository.findById(input.getAthleteId())
                        .orElseThrow(() -> new IllegalArgumentException("No athlete found with id " + input.getAthleteId()));
            }
            PenaltyKick kick = existingByKickOrder.getOrDefault(input.getKickOrder(), new PenaltyKick());
            kick.setAthlete(athlete);
            kick.setKickOrder(input.getKickOrder());
            kick.setForTeam(input.isForTeam());
            kick.setScored(input.isScored());
            kicks.add(kick);
        }
        shootout.setKicks(kicks);
    }

    private PenaltyShootoutSummaryDto toSummaryDto(PenaltyShootout s) {
        return new PenaltyShootoutSummaryDto(s.getId(), s.getTitle(), s.getCompetition(), s.getTeamName(),
                s.getOpponentName(), s.getTeamCrestUrl(), s.getOpponentCrestUrl(), s.getTeamPensScored(),
                s.getOpponentPensScored(), s.getMatchDate(), s.getMaxStrikes(), s.getKicks().size());
    }

    private PenaltyShootoutAdminDetailDto toDetailDto(PenaltyShootout shootout) {
        PenaltyShootoutAdminDetailDto dto = new PenaltyShootoutAdminDetailDto();
        dto.setId(shootout.getId());
        dto.setTitle(shootout.getTitle());
        dto.setCompetition(shootout.getCompetition());
        dto.setMatchDate(shootout.getMatchDate());
        dto.setTeamName(shootout.getTeamName());
        dto.setTeamCrestUrl(shootout.getTeamCrestUrl());
        dto.setOpponentName(shootout.getOpponentName());
        dto.setOpponentCrestUrl(shootout.getOpponentCrestUrl());
        dto.setTeamPensScored(shootout.getTeamPensScored());
        dto.setOpponentPensScored(shootout.getOpponentPensScored());
        dto.setMaxStrikes(shootout.getMaxStrikes());

        Set<Athlete> distinctAthletes = new java.util.LinkedHashSet<>();
        shootout.getCandidates().forEach(c -> distinctAthletes.add(c.getAthlete()));
        shootout.getKicks().forEach(k -> distinctAthletes.add(k.getAthlete()));
        Map<Long, com.quizapp.dto.AthleteDto> athleteDtoById =
                athleteService.toDtosWithPhotos(new java.util.ArrayList<>(distinctAthletes)).stream()
                        .collect(Collectors.toMap(com.quizapp.dto.AthleteDto::getId, a -> a));

        dto.setCandidates(shootout.getCandidates().stream()
                .map(c -> athleteDtoById.get(c.getAthlete().getId()))
                .collect(Collectors.toList()));
        dto.setKicks(shootout.getKicks().stream()
                .sorted(java.util.Comparator.comparingInt(PenaltyKick::getKickOrder))
                .map(k -> new PenaltyShootoutAdminDetailDto.KickDetail(
                        k.getId(), athleteDtoById.get(k.getAthlete().getId()), k.getKickOrder(), k.isForTeam(), k.isScored()))
                .collect(Collectors.toList()));
        return dto;
    }

    private PenaltyShootout findShootout(Long id) {
        return penaltyShootoutRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No penalty shootout found with id " + id));
    }
}
