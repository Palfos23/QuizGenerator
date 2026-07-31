package com.quizapp.service;

import com.quizapp.dto.AthleteDto;
import com.quizapp.exception.ResourceNotFoundException;
import com.quizapp.model.Athlete;
import com.quizapp.model.Grid;
import com.quizapp.model.Sport;
import com.quizapp.repository.AthleteRepository;
import com.quizapp.repository.GridCandidateRepository;
import com.quizapp.repository.GridEntryRepository;
import com.quizapp.repository.GridRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AthleteService {

    private final AthleteRepository athleteRepository;
    private final GridCandidateRepository gridCandidateRepository;
    private final GridEntryRepository gridEntryRepository;
    private final GridRepository gridRepository;

    public AthleteService(AthleteRepository athleteRepository, GridCandidateRepository gridCandidateRepository,
                           GridEntryRepository gridEntryRepository, GridRepository gridRepository) {
        this.athleteRepository = athleteRepository;
        this.gridCandidateRepository = gridCandidateRepository;
        this.gridEntryRepository = gridEntryRepository;
        this.gridRepository = gridRepository;
    }

    @Transactional(readOnly = true)
    public List<AthleteDto> findAll() {
        return athleteRepository.findAll().stream().map(AthleteService::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AthleteDto> search(Sport sport, String team, String nameContains) {
        List<Athlete> pool = sport != null ? athleteRepository.findBySport(sport) : athleteRepository.findAll();
        return pool.stream()
                .filter(a -> team == null || team.isBlank() || (a.getTeam() != null && a.getTeam().equalsIgnoreCase(team)))
                .filter(a -> nameContains == null || nameContains.isBlank()
                        || a.getName().toLowerCase().contains(nameContains.toLowerCase()))
                .map(AthleteService::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public AthleteDto create(AthleteDto dto) {
        Athlete athlete = new Athlete();
        athlete.setName(dto.getName());
        athlete.setSport(dto.getSport());
        athlete.setTeam(dto.getTeam());
        athlete.setPhotoUrl(dto.getPhotoUrl());
        return toDto(athleteRepository.save(athlete));
    }

    @Transactional
    public AthleteDto update(Long id, AthleteDto dto) {
        Athlete athlete = athleteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No athlete found with id " + id));
        athlete.setName(dto.getName());
        athlete.setSport(dto.getSport());
        athlete.setTeam(dto.getTeam());
        athlete.setPhotoUrl(dto.getPhotoUrl());
        return toDto(athleteRepository.save(athlete));
    }

    private List<Grid> findGridsReferencingAthlete(Long athleteId) {
        java.util.Map<Long, Grid> byId = new java.util.LinkedHashMap<>();
        gridRepository.findByCandidateAthleteId(athleteId).forEach(g -> byId.put(g.getId(), g));
        gridRepository.findByEntryAthleteId(athleteId).forEach(g -> byId.put(g.getId(), g));
        return new java.util.ArrayList<>(byId.values());
    }

    @Transactional(readOnly = true)
    public List<com.quizapp.dto.AthleteGridUsageDto> findGridUsage(Long athleteId) {
        return findGridsReferencingAthlete(athleteId).stream()
                .map(g -> new com.quizapp.dto.AthleteGridUsageDto(
                        g.getId(), g.getTitle(),
                        g.getEntries().stream().anyMatch(e -> e.getAthlete().getId().equals(athleteId))))
                .collect(Collectors.toList());
    }

    @Transactional
    public void delete(Long id, boolean removeFromGrids) {
        if (!athleteRepository.existsById(id)) {
            throw new ResourceNotFoundException("No athlete found with id " + id);
        }
        if (gridCandidateRepository.existsByAthlete_Id(id)) {
            if (!removeFromGrids) {
                throw new IllegalArgumentException(
                        "This athlete is used in one or more grids - remove them from those grids first.");
            }
            // Direct delete statements, not collection-based removal (load the
            // collection, remove an element, let Hibernate's orphanRemoval figure
            // out the SQL). That approach proved unreliable across several attempts
            // for this exact scenario - a plain DELETE ... WHERE athlete_id = ? has
            // no ambiguity for Hibernate to get wrong.
            gridEntryRepository.deleteByAthlete_Id(id);
            gridCandidateRepository.deleteByAthlete_Id(id);
        }
        athleteRepository.deleteById(id);
    }

    static AthleteDto toDto(Athlete a) {
        AthleteDto dto = new AthleteDto();
        dto.setId(a.getId());
        dto.setName(a.getName());
        dto.setSport(a.getSport());
        dto.setTeam(a.getTeam());
        dto.setPhotoUrl(a.getPhotoUrl());
        return dto;
    }
}
