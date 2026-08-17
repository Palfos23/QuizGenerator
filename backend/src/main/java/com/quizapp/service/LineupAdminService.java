package com.quizapp.service;

import com.quizapp.dto.LineupAdminDetailDto;
import com.quizapp.dto.LineupEntryInputDto;
import com.quizapp.dto.LineupRequest;
import com.quizapp.dto.LineupSummaryDto;
import com.quizapp.exception.ResourceNotFoundException;
import com.quizapp.model.Athlete;
import com.quizapp.model.AthletePool;
import com.quizapp.model.Lineup;
import com.quizapp.model.LineupCandidate;
import com.quizapp.model.LineupEntry;
import com.quizapp.repository.AthletePoolRepository;
import com.quizapp.repository.AthleteRepository;
import com.quizapp.repository.LineupRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class LineupAdminService {

    private static final String DEFAULT_KIT_COLOR = "#d92332";
    private static final String DEFAULT_GK_KIT_COLOR = "#f2c230";

    private final LineupRepository lineupRepository;
    private final AthleteRepository athleteRepository;
    private final AthletePoolRepository athletePoolRepository;
    private final AthleteService athleteService;

    public LineupAdminService(LineupRepository lineupRepository, AthleteRepository athleteRepository,
                               AthletePoolRepository athletePoolRepository, AthleteService athleteService) {
        this.lineupRepository = lineupRepository;
        this.athleteRepository = athleteRepository;
        this.athletePoolRepository = athletePoolRepository;
        this.athleteService = athleteService;
    }

    @Transactional(readOnly = true)
    public List<LineupSummaryDto> findAll() {
        return lineupRepository.findAll().stream()
                .sorted((a, b) -> b.getWeekStartDate().compareTo(a.getWeekStartDate()))
                .map(l -> {
                    LineupSummaryDto dto = new LineupSummaryDto(l.getId(), l.getTitle(), l.getCompetition(),
                            l.getTeamName(), l.getOpponentName(), l.getScoreFor(), l.getScoreAgainst(),
                            l.getMatchDate(), l.getWeekStartDate(), l.getFormation());
                    dto.setMaxStrikes(l.getMaxStrikes());
                    dto.setExcludedFromBattle(l.isExcludedFromBattle());
                    dto.setEntireCategoryPool(l.isEntireCategoryPool());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public LineupAdminDetailDto getOne(Long id) {
        Lineup lineup = lineupRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No Starting XI board found with id " + id));
        return toDetailDto(lineup);
    }

    @Transactional
    public LineupAdminDetailDto create(LineupRequest request) {
        Lineup lineup = new Lineup();
        applyRequest(lineup, request);
        return toDetailDto(lineupRepository.save(lineup));
    }

    @Transactional
    public LineupAdminDetailDto update(Long id, LineupRequest request) {
        Lineup lineup = lineupRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No Starting XI board found with id " + id));
        applyRequest(lineup, request);
        return toDetailDto(lineupRepository.save(lineup));
    }

    @Transactional
    public void delete(Long id) {
        if (!lineupRepository.existsById(id)) {
            throw new ResourceNotFoundException("No Starting XI board found with id " + id);
        }
        lineupRepository.deleteById(id);
    }

    private void applyRequest(Lineup lineup, LineupRequest request) {
        if (!Formations.isKnown(request.getFormation())) {
            throw new IllegalArgumentException("Unknown formation '" + request.getFormation() + "'.");
        }
        int slotCount = Formations.slotCount(request.getFormation());
        if (request.getEntries().size() != slotCount) {
            throw new IllegalArgumentException(
                    "Formation " + request.getFormation() + " needs exactly " + slotCount + " starters - got "
                            + request.getEntries().size() + ".");
        }
        Set<Integer> seenSlots = new HashSet<>();
        for (LineupEntryInputDto e : request.getEntries()) {
            if (e.getSlotIndex() < 0 || e.getSlotIndex() >= slotCount) {
                throw new IllegalArgumentException("Slot index " + e.getSlotIndex() + " is out of range for " + request.getFormation() + ".");
            }
            if (!seenSlots.add(e.getSlotIndex())) {
                throw new IllegalArgumentException("Two starters can't share formation slot " + e.getSlotIndex() + ".");
            }
        }

        lineup.setTitle(request.getTitle());
        lineup.setCompetition(request.getCompetition());
        lineup.setMatchDate(request.getMatchDate());
        lineup.setWeekStartDate(request.getWeekStartDate());
        lineup.setFormation(request.getFormation());
        lineup.setTeamName(request.getTeamName());
        lineup.setTeamCrestUrl(request.getTeamCrestUrl());
        lineup.setOpponentName(request.getOpponentName());
        lineup.setOpponentCrestUrl(request.getOpponentCrestUrl());
        lineup.setScoreFor(request.getScoreFor());
        lineup.setScoreAgainst(request.getScoreAgainst());
        lineup.setMaxStrikes(request.getMaxStrikes());
        lineup.setExcludedFromBattle(request.isExcludedFromBattle());
        lineup.setKitColor(request.getKitColor() != null && !request.getKitColor().isBlank()
                ? request.getKitColor() : DEFAULT_KIT_COLOR);
        lineup.setGoalkeeperKitColor(request.getGoalkeeperKitColor() != null && !request.getGoalkeeperKitColor().isBlank()
                ? request.getGoalkeeperKitColor() : DEFAULT_GK_KIT_COLOR);
        lineup.setEntireCategoryPool(request.isEntireCategoryPool());

        if (request.getLinkedPoolIds() != null && !request.getLinkedPoolIds().isEmpty()) {
            List<AthletePool> pools = athletePoolRepository.findAllById(request.getLinkedPoolIds());
            Set<AthletePool> merged = new HashSet<>(lineup.getLinkedPools());
            merged.addAll(pools);
            lineup.setLinkedPools(merged);
        }

        // Empty (not null) when entireCategoryPool is on - same convention as
        // GridAdminService.applyRequest.
        List<Long> candidateAthleteIds = request.getCandidateAthleteIds() == null
                ? List.of()
                : request.getCandidateAthleteIds().stream().distinct().collect(Collectors.toList());
        if (!request.isEntireCategoryPool() && candidateAthleteIds.isEmpty()) {
            throw new IllegalArgumentException(
                    "Add at least one candidate, or switch on \"every subject in this category\".");
        }
        List<Athlete> foundAthletes = athleteRepository.findAllById(candidateAthleteIds);
        Map<Long, Athlete> athleteById = foundAthletes.stream()
                .collect(Collectors.toMap(Athlete::getId, a -> a));
        if (athleteById.size() != candidateAthleteIds.size()) {
            List<Long> missing = candidateAthleteIds.stream()
                    .filter(aid -> !athleteById.containsKey(aid))
                    .collect(Collectors.toList());
            throw new IllegalArgumentException("No athlete found with id(s) " + missing);
        }

        if (request.isEntireCategoryPool()) {
            // No explicit candidate rows at all - LineupPlayService.searchCandidates()
            // queries every Athlete in Lineup.CATEGORY live instead. Clearing here also
            // reclaims storage for a board switching INTO this mode from an explicit
            // candidate list it had before.
            lineup.setCandidates(Set.of());
        } else {
            // Reuse existing candidate/entry rows for the same athlete rather than
            // deleting and recreating every save - same reasoning as GridAdminService,
            // and matters here too since a LineupBattleSolvedEntry references a
            // LineupEntry id that must survive a routine edit.
            Map<Long, LineupCandidate> existingCandidateByAthleteId = lineup.getCandidates().stream()
                    .collect(Collectors.toMap(c -> c.getAthlete().getId(), c -> c, (a, b) -> a));
            Set<LineupCandidate> candidates = candidateAthleteIds.stream()
                    .map(athleteId -> {
                        LineupCandidate c = existingCandidateByAthleteId.getOrDefault(athleteId, new LineupCandidate());
                        c.setAthlete(athleteById.get(athleteId));
                        return c;
                    })
                    .collect(Collectors.toSet());
            lineup.setCandidates(candidates);
        }

        Map<Long, LineupEntry> existingByAthleteId = lineup.getEntries().stream()
                .collect(Collectors.toMap(e -> e.getAthlete().getId(), e -> e, (a, b) -> a));

        Set<LineupEntry> entries = new HashSet<>();
        for (LineupEntryInputDto input : request.getEntries()) {
            Athlete athlete = athleteById.get(input.getAthleteId());
            if (athlete == null) {
                athlete = athleteRepository.findById(input.getAthleteId())
                        .orElseThrow(() -> new IllegalArgumentException("No athlete found with id " + input.getAthleteId()));
            }
            LineupEntry entry = existingByAthleteId.getOrDefault(input.getAthleteId(), new LineupEntry());
            entry.setAthlete(athlete);
            entry.setShirtNumber(input.getShirtNumber());
            entry.setSlotIndex(input.getSlotIndex());
            entry.setCaptain(input.isCaptain());
            entries.add(entry);
        }
        lineup.setEntries(entries);
    }

    private LineupAdminDetailDto toDetailDto(Lineup lineup) {
        LineupAdminDetailDto dto = new LineupAdminDetailDto();
        dto.setId(lineup.getId());
        dto.setTitle(lineup.getTitle());
        dto.setCompetition(lineup.getCompetition());
        dto.setMatchDate(lineup.getMatchDate());
        dto.setWeekStartDate(lineup.getWeekStartDate());
        dto.setFormation(lineup.getFormation());
        dto.setTeamName(lineup.getTeamName());
        dto.setTeamCrestUrl(lineup.getTeamCrestUrl());
        dto.setOpponentName(lineup.getOpponentName());
        dto.setOpponentCrestUrl(lineup.getOpponentCrestUrl());
        dto.setScoreFor(lineup.getScoreFor());
        dto.setScoreAgainst(lineup.getScoreAgainst());
        dto.setMaxStrikes(lineup.getMaxStrikes());
        dto.setExcludedFromBattle(lineup.isExcludedFromBattle());
        dto.setKitColor(lineup.getKitColor() != null ? lineup.getKitColor() : DEFAULT_KIT_COLOR);
        dto.setGoalkeeperKitColor(lineup.getGoalkeeperKitColor() != null ? lineup.getGoalkeeperKitColor() : DEFAULT_GK_KIT_COLOR);
        dto.setEntireCategoryPool(lineup.isEntireCategoryPool());

        java.util.Set<Athlete> distinctAthletes = new java.util.LinkedHashSet<>();
        lineup.getCandidates().forEach(c -> distinctAthletes.add(c.getAthlete()));
        lineup.getEntries().forEach(e -> distinctAthletes.add(e.getAthlete()));
        Map<Long, com.quizapp.dto.AthleteDto> athleteDtoById =
                athleteService.toDtosWithPhotos(new java.util.ArrayList<>(distinctAthletes)).stream()
                        .collect(Collectors.toMap(com.quizapp.dto.AthleteDto::getId, a -> a));

        dto.setCandidates(lineup.getCandidates().stream()
                .map(c -> athleteDtoById.get(c.getAthlete().getId()))
                .collect(Collectors.toList()));
        dto.setEntries(lineup.getEntries().stream()
                .sorted(java.util.Comparator.comparingInt(LineupEntry::getSlotIndex))
                .map(e -> new LineupAdminDetailDto.EntryDetail(
                        e.getId(), athleteDtoById.get(e.getAthlete().getId()), e.getShirtNumber(), e.getSlotIndex(),
                        e.isCaptain()))
                .collect(Collectors.toList()));
        return dto;
    }
}
