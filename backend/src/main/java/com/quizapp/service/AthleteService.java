package com.quizapp.service;

import com.quizapp.dto.AthleteDto;
import com.quizapp.exception.ResourceNotFoundException;
import com.quizapp.model.Athlete;
import com.quizapp.model.Sport;
import com.quizapp.repository.AthleteRepository;
import com.quizapp.repository.GridCandidateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AthleteService {

    private final AthleteRepository athleteRepository;
    private final GridCandidateRepository gridCandidateRepository;

    public AthleteService(AthleteRepository athleteRepository, GridCandidateRepository gridCandidateRepository) {
        this.athleteRepository = athleteRepository;
        this.gridCandidateRepository = gridCandidateRepository;
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

    @Transactional
    public void delete(Long id) {
        if (!athleteRepository.existsById(id)) {
            throw new ResourceNotFoundException("No athlete found with id " + id);
        }
        if (gridCandidateRepository.existsByAthlete_Id(id)) {
            throw new IllegalArgumentException(
                    "This athlete is used in one or more grids - remove them from those grids first.");
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
