package com.quizapp.service;

import com.quizapp.dto.*;
import com.quizapp.exception.ResourceNotFoundException;
import com.quizapp.model.Athlete;
import com.quizapp.model.AthletePool;
import com.quizapp.model.Club;
import com.quizapp.model.Grid;
import com.quizapp.model.GridCandidate;
import com.quizapp.model.GridEntry;
import com.quizapp.repository.AthletePoolRepository;
import com.quizapp.repository.AthleteRepository;
import com.quizapp.repository.ClubRepository;
import com.quizapp.repository.GridRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class GridAdminService {

    private final GridRepository gridRepository;
    private final AthleteRepository athleteRepository;
    private final ClubRepository clubRepository;
    private final AthletePoolRepository athletePoolRepository;

    public GridAdminService(GridRepository gridRepository, AthleteRepository athleteRepository,
                             ClubRepository clubRepository, AthletePoolRepository athletePoolRepository) {
        this.gridRepository = gridRepository;
        this.athleteRepository = athleteRepository;
        this.clubRepository = clubRepository;
        this.athletePoolRepository = athletePoolRepository;
    }

    @Transactional(readOnly = true)
    public List<GridSummaryDto> findAll() {
        return gridRepository.findAll().stream()
                .sorted((a, b) -> a.getWeekStartDate().compareTo(b.getWeekStartDate()))
                .map(g -> {
                    GridSummaryDto dto = new GridSummaryDto(g.getId(), g.getTitle(), g.getSport(), g.getWeekStartDate(),
                            g.getEntries().size(), null, null);
                    dto.setMaxStrikes(g.getMaxStrikes());
                    dto.setExcludedFromGridBattle(g.isExcludedFromGridBattle());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public GridAdminDetailDto getOne(Long id) {
        Grid grid = gridRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No grid found with id " + id));
        return toDetailDto(grid);
    }

    @Transactional
    public GridAdminDetailDto create(GridRequest request) {
        validateDateNotTaken(request.getWeekStartDate(), null);
        Grid grid = new Grid();
        applyRequest(grid, request);
        return toDetailDto(gridRepository.save(grid));
    }

    @Transactional
    public GridAdminDetailDto update(Long id, GridRequest request) {
        validateDateNotTaken(request.getWeekStartDate(), id);
        Grid grid = gridRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No grid found with id " + id));
        applyRequest(grid, request);
        return toDetailDto(gridRepository.save(grid));
    }

    // excludeGridId lets an edit keep its own existing date without flagging
    // itself as a conflict.
    private void validateDateNotTaken(java.time.LocalDate weekStartDate, Long excludeGridId) {
        boolean taken = gridRepository.findByWeekStartDate(weekStartDate).stream()
                .anyMatch(g -> !g.getId().equals(excludeGridId));
        if (taken) {
            throw new IllegalStateException("Another grid already uses the week of " + weekStartDate + ".");
        }
    }

    @Transactional
    public void delete(Long id) {
        if (!gridRepository.existsById(id)) {
            throw new ResourceNotFoundException("No grid found with id " + id);
        }
        gridRepository.deleteById(id);
    }

    private void applyRequest(Grid grid, GridRequest request) {
        grid.setTitle(request.getTitle());
        grid.setTheme(request.getTheme());
        grid.setSport(request.getSport());
        grid.setWeekStartDate(request.getWeekStartDate());
        grid.setMaxStrikes(request.getMaxStrikes());
        grid.setSortAscending(request.isSortAscending());
        grid.setRanked(request.isRanked());
        grid.setExcludedFromGridBattle(request.isExcludedFromGridBattle());

        if (request.getLinkedPoolIds() != null && !request.getLinkedPoolIds().isEmpty()) {
            List<AthletePool> pools = athletePoolRepository.findAllById(request.getLinkedPoolIds());
            Set<AthletePool> merged = new HashSet<>(grid.getLinkedPools());
            merged.addAll(pools);
            grid.setLinkedPools(merged);
        }

        // Single batch fetch instead of one query per athlete - looking each one
        // up individually is exactly the kind of thing that gets slower the bigger
        // the candidate pool is, which matches what a large grid was experiencing.
        // Deduplicated defensively - a repeated id here would otherwise make the
        // same reused GridCandidate get processed twice below.
        List<Long> candidateAthleteIds = request.getCandidateAthleteIds().stream()
                .distinct()
                .collect(Collectors.toList());
        List<Athlete> foundAthletes = athleteRepository.findAllById(candidateAthleteIds);
        Map<Long, Athlete> athleteById = foundAthletes.stream()
                .collect(Collectors.toMap(Athlete::getId, a -> a));
        if (athleteById.size() != candidateAthleteIds.size()) {
            List<Long> missing = candidateAthleteIds.stream()
                    .filter(id -> !athleteById.containsKey(id))
                    .collect(Collectors.toList());
            throw new IllegalArgumentException("No athlete found with id(s) " + missing);
        }

        // Reuse existing candidate rows for athletes that are still in the pool -
        // deleting and recreating every single one on every save (even when nothing
        // changed) is what made saving slow with a large candidate pool.
        Map<Long, GridCandidate> existingCandidateByAthleteId = grid.getCandidates().stream()
                .collect(Collectors.toMap(c -> c.getAthlete().getId(), c -> c, (a, b) -> a));

        Set<GridCandidate> candidates = candidateAthleteIds.stream()
                .map(athleteId -> {
                    GridCandidate c = existingCandidateByAthleteId.getOrDefault(athleteId, new GridCandidate());
                    c.setAthlete(athleteById.get(athleteId));
                    return c;
                })
                .collect(Collectors.toSet());
        if (candidates.stream().anyMatch(c -> c.getAthlete() == null)) {
            throw new IllegalStateException("Internal error building the candidate list - please try saving again.");
        }
        grid.setCandidates(candidates);

        // Match against existing entries by athlete so a routine hint/value edit
        // preserves each entry's id - solvedEntryIds on every player's saved
        // GridAttempt references these ids, so recreating them from scratch here
        // would silently disconnect everyone's progress on this grid.
        Map<Long, GridEntry> existingByAthleteId = grid.getEntries().stream()
                .collect(Collectors.toMap(e -> e.getAthlete().getId(), e -> e, (a, b) -> a));

        int index = 0;
        Set<GridEntry> entries = new HashSet<>();
        for (GridEntryInputDto input : request.getEntries()) {
            Athlete athlete = athleteById.get(input.getAthleteId());
            if (athlete == null) {
                // allow an entry's athlete even if the admin forgot to also list it as a
                // candidate - fetch it directly rather than rejecting the whole grid
                athlete = athleteRepository.findById(input.getAthleteId())
                        .orElseThrow(() -> new IllegalArgumentException("No athlete found with id " + input.getAthleteId()));
            }
            GridEntry entry = existingByAthleteId.getOrDefault(input.getAthleteId(), new GridEntry());
            entry.setAthlete(athlete);
            entry.setHintLabel(input.getHintLabel());
            if (request.isRanked()) {
                if (input.getHintValue() == null) {
                    throw new IllegalArgumentException(
                            "'" + athlete.getName() + "' is missing a hint value - required for a ranked grid.");
                }
                entry.setHintValue(input.getHintValue());
            } else {
                // Unranked grids don't sort or display a number at all - force
                // null here rather than trust the payload, so a stale value
                // from before an admin switched a grid to unranked can't
                // linger unused in the database.
                entry.setHintValue(null);
            }
            entry.setOrderIndex(index++);

            if (input.getClubId() != null) {
                Club club = clubRepository.findById(input.getClubId())
                        .orElseThrow(() -> new IllegalArgumentException("No club found with id " + input.getClubId()));
                entry.setClub(club);
            } else {
                entry.setClub(null);
            }
            entry.setShowLogo(input.getShowLogo());
            entry.setUseOwnPhotoAsLogo(input.isUseOwnPhotoAsLogo());

            entries.add(entry);
        }
        grid.setEntries(entries);
    }

    private GridAdminDetailDto toDetailDto(Grid grid) {
        GridAdminDetailDto dto = new GridAdminDetailDto();
        dto.setId(grid.getId());
        dto.setTitle(grid.getTitle());
        dto.setTheme(grid.getTheme());
        dto.setSport(grid.getSport());
        dto.setWeekStartDate(grid.getWeekStartDate());
        dto.setMaxStrikes(grid.getMaxStrikes());
        dto.setSortAscending(grid.isSortAscending());
        dto.setRanked(grid.isRanked());
        dto.setExcludedFromGridBattle(grid.isExcludedFromGridBattle());
        dto.setCandidates(grid.getCandidates().stream()
                .map(c -> AthleteService.toDto(c.getAthlete()))
                .collect(Collectors.toList()));
        dto.setEntries(grid.getEntries().stream()
                .map(e -> new GridAdminDetailDto.EntryDetail(
                        e.getId(), AthleteService.toDto(e.getAthlete()), e.getHintLabel(), e.getHintValue(),
                        e.getClub() != null ? ClubService.toDto(e.getClub()) : null, e.isShowLogo(),
                        e.isUseOwnPhotoAsLogo()))
                .collect(Collectors.toList()));
        return dto;
    }
}
