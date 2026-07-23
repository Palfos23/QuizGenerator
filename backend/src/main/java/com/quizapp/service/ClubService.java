package com.quizapp.service;

import com.quizapp.dto.ClubDto;
import com.quizapp.exception.ResourceNotFoundException;
import com.quizapp.model.Club;
import com.quizapp.model.Sport;
import com.quizapp.repository.ClubRepository;
import com.quizapp.repository.GridEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ClubService {

    private final ClubRepository clubRepository;
    private final GridEntryRepository gridEntryRepository;

    public ClubService(ClubRepository clubRepository, GridEntryRepository gridEntryRepository) {
        this.clubRepository = clubRepository;
        this.gridEntryRepository = gridEntryRepository;
    }

    @Transactional(readOnly = true)
    public List<ClubDto> search(Sport sport, String nameContains) {
        List<Club> pool = sport != null ? clubRepository.findBySport(sport) : clubRepository.findAll();
        return pool.stream()
                .filter(c -> nameContains == null || nameContains.isBlank()
                        || c.getName().toLowerCase().contains(nameContains.toLowerCase()))
                .map(ClubService::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public ClubDto create(ClubDto dto) {
        Club club = new Club();
        club.setName(dto.getName());
        club.setSport(dto.getSport());
        club.setLogoUrl(dto.getLogoUrl());
        club.setColor(dto.getColor());
        return toDto(clubRepository.save(club));
    }

    @Transactional
    public ClubDto update(Long id, ClubDto dto) {
        Club club = clubRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No club found with id " + id));
        club.setName(dto.getName());
        club.setSport(dto.getSport());
        club.setLogoUrl(dto.getLogoUrl());
        club.setColor(dto.getColor());
        return toDto(clubRepository.save(club));
    }

    @Transactional
    public void delete(Long id) {
        if (!clubRepository.existsById(id)) {
            throw new ResourceNotFoundException("No club found with id " + id);
        }
        if (gridEntryRepository.existsByClub_Id(id)) {
            throw new IllegalArgumentException(
                    "This club is used as a logo on one or more grid entries - remove it from those first.");
        }
        clubRepository.deleteById(id);
    }

    static ClubDto toDto(Club c) {
        ClubDto dto = new ClubDto();
        dto.setId(c.getId());
        dto.setName(c.getName());
        dto.setSport(c.getSport());
        dto.setLogoUrl(c.getLogoUrl());
        dto.setColor(c.getColor());
        return dto;
    }
}
