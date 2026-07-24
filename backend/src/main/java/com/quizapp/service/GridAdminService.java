package com.quizapp.service;

import com.quizapp.dto.*;
import com.quizapp.exception.ResourceNotFoundException;
import com.quizapp.model.Athlete;
import com.quizapp.model.Club;
import com.quizapp.model.Grid;
import com.quizapp.model.GridCandidate;
import com.quizapp.model.GridEntry;
import com.quizapp.repository.AthleteRepository;
import com.quizapp.repository.ClubRepository;
import com.quizapp.repository.GridRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class GridAdminService {

    private final GridRepository gridRepository;
    private final AthleteRepository athleteRepository;
    private final ClubRepository clubRepository;

    public GridAdminService(GridRepository gridRepository, AthleteRepository athleteRepository,
                             ClubRepository clubRepository) {
        this.gridRepository = gridRepository;
        this.athleteRepository = athleteRepository;
        this.clubRepository = clubRepository;
    }

    @Transactional(readOnly = true)
    public List<GridSummaryDto> findAll() {
        return gridRepository.findAll().stream()
                .map(g -> new GridSummaryDto(g.getId(), g.getTitle(), g.getSport(), g.getWeekStartDate(), g.getEntries().size(), null, null))
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
        Grid grid = new Grid();
        applyRequest(grid, request);
        return toDetailDto(gridRepository.save(grid));
    }

    @Transactional
    public GridAdminDetailDto update(Long id, GridRequest request) {
        Grid grid = gridRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No grid found with id " + id));
        applyRequest(grid, request);
        return toDetailDto(gridRepository.save(grid));
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

        Map<Long, Athlete> athleteById = new HashMap<>();
        for (Long athleteId : request.getCandidateAthleteIds()) {
            athleteById.put(athleteId, athleteRepository.findById(athleteId)
                    .orElseThrow(() -> new IllegalArgumentException("No athlete found with id " + athleteId)));
        }

        List<GridCandidate> candidates = request.getCandidateAthleteIds().stream()
                .map(athleteId -> {
                    GridCandidate c = new GridCandidate();
                    c.setAthlete(athleteById.get(athleteId));
                    return c;
                })
                .collect(Collectors.toList());
        grid.setCandidates(candidates);

        int index = 0;
        List<GridEntry> entries = new ArrayList<>();
        for (GridEntryInputDto input : request.getEntries()) {
            Athlete athlete = athleteById.get(input.getAthleteId());
            if (athlete == null) {
                // allow an entry's athlete even if the admin forgot to also list it as a
                // candidate - fetch it directly rather than rejecting the whole grid
                athlete = athleteRepository.findById(input.getAthleteId())
                        .orElseThrow(() -> new IllegalArgumentException("No athlete found with id " + input.getAthleteId()));
            }
            GridEntry entry = new GridEntry();
            entry.setAthlete(athlete);
            entry.setHintLabel(input.getHintLabel());
            entry.setHintValue(input.getHintValue());
            entry.setOrderIndex(index++);

            if (input.getClubId() != null) {
                Club club = clubRepository.findById(input.getClubId())
                        .orElseThrow(() -> new IllegalArgumentException("No club found with id " + input.getClubId()));
                entry.setClub(club);
            }
            entry.setShowLogo(input.getShowLogo());

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
        dto.setCandidates(grid.getCandidates().stream()
                .map(c -> AthleteService.toDto(c.getAthlete()))
                .collect(Collectors.toList()));
        dto.setEntries(grid.getEntries().stream()
                .map(e -> new GridAdminDetailDto.EntryDetail(
                        e.getId(), AthleteService.toDto(e.getAthlete()), e.getHintLabel(), e.getHintValue(),
                        e.getClub() != null ? ClubService.toDto(e.getClub()) : null, e.isShowLogo()))
                .collect(Collectors.toList()));
        return dto;
    }
}
