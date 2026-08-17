package com.quizapp.service;

import com.quizapp.dto.AthleteDto;
import com.quizapp.dto.LineupGuessRequest;
import com.quizapp.dto.LineupGuessResultDto;
import com.quizapp.dto.LineupPlayStateDto;
import com.quizapp.dto.LineupSlotDto;
import com.quizapp.dto.LineupSummaryDto;
import com.quizapp.exception.ResourceNotFoundException;
import com.quizapp.model.Athlete;
import com.quizapp.model.Lineup;
import com.quizapp.model.LineupCandidate;
import com.quizapp.model.LineupEntry;
import com.quizapp.repository.AthleteRepository;
import com.quizapp.repository.LineupRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

// Solo (and local pass-and-play) Starting XI is entirely stateless server-side
// - same reasoning as Grid's multiplayer-start/-guess/-reveal trio in
// GridPlayService: nothing here needs a single logged-in "attempt" to attach
// to, so the client just tracks its own guessed slots and reports them back
// on every call.
@Service
public class LineupPlayService {

    private static final String DEFAULT_KIT_COLOR = "#d92332";
    private static final String DEFAULT_GK_KIT_COLOR = "#f2c230";

    private final LineupRepository lineupRepository;
    private final AthleteRepository athleteRepository;

    public LineupPlayService(LineupRepository lineupRepository, AthleteRepository athleteRepository) {
        this.lineupRepository = lineupRepository;
        this.athleteRepository = athleteRepository;
    }

    @Transactional(readOnly = true)
    public List<LineupSummaryDto> findAll() {
        LocalDate today = LocalDate.now();
        return lineupRepository.findByWeekStartDateLessThanEqualOrderByWeekStartDateDescIdDesc(today).stream()
                .map(l -> new LineupSummaryDto(l.getId(), l.getTitle(), l.getCompetition(), l.getTeamName(),
                        l.getOpponentName(), l.getScoreFor(), l.getScoreAgainst(), l.getMatchDate(),
                        l.getWeekStartDate(), l.getFormation()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public LineupPlayStateDto getStartState(Long lineupId) {
        Lineup lineup = findLineup(lineupId);
        LineupPlayStateDto dto = new LineupPlayStateDto();
        dto.setId(lineup.getId());
        dto.setTitle(lineup.getTitle());
        dto.setCompetition(lineup.getCompetition());
        dto.setTeamName(lineup.getTeamName());
        dto.setTeamCrestUrl(lineup.getTeamCrestUrl());
        dto.setOpponentName(lineup.getOpponentName());
        dto.setOpponentCrestUrl(lineup.getOpponentCrestUrl());
        dto.setScoreFor(lineup.getScoreFor());
        dto.setScoreAgainst(lineup.getScoreAgainst());
        dto.setMatchDate(lineup.getMatchDate());
        dto.setFormation(lineup.getFormation());
        dto.setMaxStrikes(lineup.getMaxStrikes());
        dto.setKitColor(lineup.getKitColor() != null ? lineup.getKitColor() : DEFAULT_KIT_COLOR);
        dto.setGoalkeeperKitColor(lineup.getGoalkeeperKitColor() != null ? lineup.getGoalkeeperKitColor() : DEFAULT_GK_KIT_COLOR);
        dto.setSlots(lineup.getEntries().stream()
                .sorted(slotOrder())
                .map(e -> new LineupSlotDto(e.getId(), e.getSlotIndex(), e.getShirtNumber(), e.isCaptain(),
                        false, null, null))
                .collect(Collectors.toList()));
        return dto;
    }

    /**
     * Same "entire category" live-search behavior as GridPlayService.searchCandidates
     * - when the board has no stored pool, every Athlete tagged Lineup.CATEGORY
     * ("Football") is queried live instead.
     */
    @Transactional(readOnly = true)
    public List<AthleteDto> searchCandidates(Long lineupId, String nameContains) {
        Lineup lineup = findLineup(lineupId);
        String term = nameContains == null ? "" : nameContains.trim().toLowerCase();
        List<Athlete> pool = lineup.isEntireCategoryPool()
                ? (term.isEmpty()
                        ? athleteRepository.findBySport(Lineup.CATEGORY)
                        : athleteRepository.findBySportAndNameContainingIgnoreCase(Lineup.CATEGORY, term))
                : lineup.getCandidates().stream().map(LineupCandidate::getAthlete).collect(Collectors.toList());
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

    @Transactional(readOnly = true)
    public LineupGuessResultDto guess(Long lineupId, LineupGuessRequest request) {
        Lineup lineup = findLineup(lineupId);
        Set<Long> revealed = request.getRevealedEntryIds() == null
                ? Collections.emptySet() : Set.copyOf(request.getRevealedEntryIds());

        LineupGuessResultDto result = new LineupGuessResultDto();
        LineupEntry matched = lineup.getEntries().stream()
                .filter(e -> !revealed.contains(e.getId()) && e.getAthlete().getId().equals(request.getAthleteId()))
                .findFirst().orElse(null);

        if (matched != null) {
            result.setCorrect(true);
            result.setSlot(new LineupSlotDto(matched.getId(), matched.getSlotIndex(), matched.getShirtNumber(),
                    matched.isCaptain(), true, matched.getAthlete().getName(), matched.getAthlete().getPhotoUrl()));
            result.setAllSolved(revealed.size() + 1 >= lineup.getEntries().size());
        } else {
            result.setCorrect(false);
            result.setAllSolved(false);
        }
        return result;
    }

    /** Every slot's answer, for revealing what wasn't found once a player gives up. */
    @Transactional(readOnly = true)
    public java.util.Map<Long, String> reveal(Long lineupId) {
        Lineup lineup = findLineup(lineupId);
        return lineup.getEntries().stream()
                .collect(Collectors.toMap(LineupEntry::getId, e -> e.getAthlete().getName()));
    }

    private Lineup findLineup(Long id) {
        return lineupRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No Starting XI board found with id " + id));
    }

    private Comparator<LineupEntry> slotOrder() {
        return Comparator.comparingInt(LineupEntry::getSlotIndex);
    }
}
