package com.quizapp.service;

import com.quizapp.dto.AthleteDto;
import com.quizapp.dto.PenaltyGuessRequest;
import com.quizapp.dto.PenaltyGuessResultDto;
import com.quizapp.dto.PenaltyKickDto;
import com.quizapp.dto.PenaltyShootoutPlayStateDto;
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

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

// Entirely stateless, solo and pass-and-play alike - see PenaltyShootout's
// class comment for why there's no persisted-attempt path the way Lineup's
// weekly solo mode has one. Both play modes start from a blank board
// (getStartState) and report their own progress back on every guess
// (guess/revealAll), the same way LineupPlayService's *multiplayer*-prefixed
// methods already work for Lineup's pass-and-play flavor - this class is
// that pattern applied as the *only* flavor, not a second one alongside a
// persisted-attempt system.
@Service
public class PenaltyShootoutPlayService {

    private final PenaltyShootoutRepository penaltyShootoutRepository;
    private final AthleteRepository athleteRepository;

    public PenaltyShootoutPlayService(PenaltyShootoutRepository penaltyShootoutRepository,
                                       AthleteRepository athleteRepository) {
        this.penaltyShootoutRepository = penaltyShootoutRepository;
        this.athleteRepository = athleteRepository;
    }

    /** The full pool, for "pick my own" board selection. */
    @Transactional(readOnly = true)
    public List<PenaltyShootoutSummaryDto> findAll() {
        return penaltyShootoutRepository.findAll().stream()
                .map(this::toSummaryDto)
                .sorted(Comparator.comparing(PenaltyShootoutSummaryDto::getTitle))
                .collect(Collectors.toList());
    }

    /** "Random" round-start picker - mirrors LineupPlayService.getBattleRoundChoices. */
    @Transactional(readOnly = true)
    public List<PenaltyShootoutSummaryDto> getRoundChoices(int count, List<Long> excludeIds) {
        List<PenaltyShootout> pool = penaltyShootoutRepository.findAll().stream()
                .filter(s -> excludeIds == null || !excludeIds.contains(s.getId()))
                .collect(Collectors.toList());
        Collections.shuffle(pool);
        return pool.stream()
                .limit(count)
                .map(this::toSummaryDto)
                .collect(Collectors.toList());
    }

    /** Starting state - every kick blank, nothing solved. No persisted attempt involved. */
    @Transactional(readOnly = true)
    public PenaltyShootoutPlayStateDto getStartState(Long shootoutId) {
        PenaltyShootout shootout = findShootout(shootoutId);
        PenaltyShootoutPlayStateDto dto = baseDto(shootout);
        dto.setKicks(shootout.getKicks().stream()
                .sorted(kickOrder())
                .map(k -> new PenaltyKickDto(k.getId(), k.getKickOrder(), k.isForTeam(), false, false, k.isScored(), null, null))
                .collect(Collectors.toList()));
        return dto;
    }

    /**
     * Search within just this board's candidate pool - same reasoning as
     * LineupPlayService.searchCandidates, minus the "entire category" mode
     * (every Penalty Shootout board has an explicit candidate list).
     */
    @Transactional(readOnly = true)
    public List<AthleteDto> searchCandidates(Long shootoutId, String nameContains) {
        PenaltyShootout shootout = findShootout(shootoutId);
        String term = nameContains == null ? "" : nameContains.trim().toLowerCase();
        List<Athlete> pool = shootout.getCandidates().stream().map(PenaltyCandidate::getAthlete).collect(Collectors.toList());
        return pool.stream()
                .filter(a -> term.isEmpty() || a.getName().toLowerCase().contains(term))
                .sorted(term.isEmpty() ? Comparator.comparing(Athlete::getName)
                        : Comparator.<Athlete>comparingInt(a -> matchRank(a.getName(), term))
                                .thenComparing(Athlete::getName))
                .map(AthleteService::toDto)
                .limit(5)
                .collect(Collectors.toList());
    }

    private int matchRank(String name, String term) {
        String lower = name.toLowerCase();
        if (lower.equals(term)) return 0;
        if (lower.startsWith(term)) return 1;
        return 2;
    }

    /** Stateless guess check - the client tracks which kicks are already revealed. */
    @Transactional(readOnly = true)
    public PenaltyGuessResultDto guess(Long shootoutId, PenaltyGuessRequest request) {
        PenaltyShootout shootout = findShootout(shootoutId);
        Set<Long> revealed = request.getRevealedKickIds() == null
                ? Collections.emptySet() : Set.copyOf(request.getRevealedKickIds());

        PenaltyGuessResultDto result = new PenaltyGuessResultDto();
        PenaltyKick matched = shootout.getKicks().stream()
                .filter(k -> !revealed.contains(k.getId()) && k.getAthlete().getId().equals(request.getAthleteId()))
                .findFirst().orElse(null);

        if (matched != null) {
            result.setCorrect(true);
            result.setKick(new PenaltyKickDto(matched.getId(), matched.getKickOrder(), matched.isForTeam(), true, true,
                    matched.isScored(), matched.getAthlete().getName(), matched.getAthlete().getPhotoUrl()));
            result.setAllSolved(revealed.size() + 1 >= shootout.getKicks().size());
        } else {
            result.setCorrect(false);
            result.setAllSolved(false);
        }
        return result;
    }

    /** Every kick's answer, for showing what wasn't found once a round has ended. */
    @Transactional(readOnly = true)
    public java.util.Map<Long, String> getReveal(Long shootoutId) {
        PenaltyShootout shootout = findShootout(shootoutId);
        return shootout.getKicks().stream()
                .collect(Collectors.toMap(PenaltyKick::getId, k -> k.getAthlete().getName()));
    }

    /** Every kick fully revealed (name + photo), for a results-screen recap. */
    @Transactional(readOnly = true)
    public List<PenaltyKickDto> revealAll(Long shootoutId) {
        PenaltyShootout shootout = findShootout(shootoutId);
        return shootout.getKicks().stream()
                .sorted(kickOrder())
                .map(k -> new PenaltyKickDto(k.getId(), k.getKickOrder(), k.isForTeam(), true, false,
                        k.isScored(), k.getAthlete().getName(), k.getAthlete().getPhotoUrl()))
                .collect(Collectors.toList());
    }

    private PenaltyShootoutSummaryDto toSummaryDto(PenaltyShootout s) {
        return new PenaltyShootoutSummaryDto(s.getId(), s.getTitle(), s.getCompetition(), s.getTeamName(),
                s.getOpponentName(), s.getTeamCrestUrl(), s.getOpponentCrestUrl(), s.getTeamPensScored(),
                s.getOpponentPensScored(), s.getMatchDate(), s.getMaxStrikes(), s.getKicks().size());
    }

    private PenaltyShootoutPlayStateDto baseDto(PenaltyShootout shootout) {
        PenaltyShootoutPlayStateDto dto = new PenaltyShootoutPlayStateDto();
        dto.setId(shootout.getId());
        dto.setTitle(shootout.getTitle());
        dto.setCompetition(shootout.getCompetition());
        dto.setTeamName(shootout.getTeamName());
        dto.setTeamCrestUrl(shootout.getTeamCrestUrl());
        dto.setOpponentName(shootout.getOpponentName());
        dto.setOpponentCrestUrl(shootout.getOpponentCrestUrl());
        dto.setTeamPensScored(shootout.getTeamPensScored());
        dto.setOpponentPensScored(shootout.getOpponentPensScored());
        dto.setMatchDate(shootout.getMatchDate());
        dto.setUpdatedAt(shootout.getUpdatedAt());
        dto.setMaxStrikes(shootout.getMaxStrikes());
        return dto;
    }

    private PenaltyShootout findShootout(Long id) {
        return penaltyShootoutRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No penalty shootout found with id " + id));
    }

    private Comparator<PenaltyKick> kickOrder() {
        return Comparator.comparingInt(PenaltyKick::getKickOrder);
    }
}
