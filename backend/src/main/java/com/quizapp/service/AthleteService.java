package com.quizapp.service;

import com.quizapp.dto.AthleteDto;
import com.quizapp.dto.AthletePhotoDto;
import com.quizapp.exception.ResourceNotFoundException;
import com.quizapp.model.Athlete;
import com.quizapp.model.AthletePhoto;
import com.quizapp.model.Grid;
import com.quizapp.repository.AthletePhotoRepository;
import com.quizapp.repository.AthleteRepository;
import com.quizapp.repository.GridCandidateRepository;
import com.quizapp.repository.GridEntryRepository;
import com.quizapp.repository.GridRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AthleteService {

    private final AthleteRepository athleteRepository;
    private final GridCandidateRepository gridCandidateRepository;
    private final GridEntryRepository gridEntryRepository;
    private final GridRepository gridRepository;
    private final AthletePhotoRepository athletePhotoRepository;

    public AthleteService(AthleteRepository athleteRepository, GridCandidateRepository gridCandidateRepository,
                           GridEntryRepository gridEntryRepository, GridRepository gridRepository,
                           AthletePhotoRepository athletePhotoRepository) {
        this.athleteRepository = athleteRepository;
        this.gridCandidateRepository = gridCandidateRepository;
        this.gridEntryRepository = gridEntryRepository;
        this.gridRepository = gridRepository;
        this.athletePhotoRepository = athletePhotoRepository;
    }

    // Wraps the plain static toDto with the athlete's additional photos -
    // used only where the photo library actually matters (the Subjects admin
    // page, the grid editor's candidate/entry picker), not every place an
    // AthleteDto gets built, to avoid an extra query per athlete in contexts
    // (player-facing search, pool listings) that never display it.
    // For a single athlete only (create/update) - for a whole list, use
    // toDtosWithPhotos below instead, which batches the query.
    AthleteDto toDtoWithPhotos(Athlete a) {
        AthleteDto dto = toDto(a);
        dto.setAdditionalPhotos(athletePhotoRepository.findByAthlete_Id(a.getId()).stream()
                .map(AthleteService::toPhotoDto)
                .collect(Collectors.toList()));
        return dto;
    }

    // Batch version for lists - one query for every athlete's photos combined,
    // instead of one query per athlete. Critical once the list is in the
    // thousands (findAll/search on the Subjects admin page) - the per-athlete
    // version above does the exact same job correctly for one item, but does
    // real damage at list scale.
    List<AthleteDto> toDtosWithPhotos(List<Athlete> athletes) {
        List<Long> ids = athletes.stream().map(Athlete::getId).collect(Collectors.toList());
        Map<Long, List<AthletePhotoDto>> photosByAthleteId = athletePhotoRepository.findByAthlete_IdIn(ids).stream()
                .collect(Collectors.groupingBy(
                        p -> p.getAthlete().getId(),
                        Collectors.mapping(AthleteService::toPhotoDto, Collectors.toList())));
        return athletes.stream().map(a -> {
            AthleteDto dto = toDto(a);
            dto.setAdditionalPhotos(photosByAthleteId.getOrDefault(a.getId(), List.of()));
            return dto;
        }).collect(Collectors.toList());
    }

    private static AthletePhotoDto toPhotoDto(AthletePhoto p) {
        AthletePhotoDto dto = new AthletePhotoDto();
        dto.setId(p.getId());
        dto.setPhotoUrl(p.getPhotoUrl());
        dto.setLabel(p.getLabel());
        return dto;
    }

    // Replaces the full set of additional photos to match what was sent -
    // same full-replacement approach as pool membership: anything not in the
    // new list gets deleted, anything without an id is newly created,
    // anything with a matching id and unchanged content is left alone.
    private void applyAdditionalPhotos(Athlete athlete, List<AthletePhotoDto> incoming) {
        List<AthletePhoto> existing = athletePhotoRepository.findByAthlete_Id(athlete.getId());
        Set<Long> incomingIds = incoming == null ? new HashSet<>() : incoming.stream()
                .map(AthletePhotoDto::getId).filter(java.util.Objects::nonNull).collect(Collectors.toSet());
        for (AthletePhoto existingPhoto : existing) {
            if (!incomingIds.contains(existingPhoto.getId())) {
                athletePhotoRepository.delete(existingPhoto);
            }
        }
        if (incoming == null) return;
        for (AthletePhotoDto photoDto : incoming) {
            if (photoDto.getId() == null) {
                AthletePhoto photo = new AthletePhoto();
                photo.setAthlete(athlete);
                photo.setPhotoUrl(photoDto.getPhotoUrl());
                photo.setLabel(photoDto.getLabel());
                athletePhotoRepository.save(photo);
            } else {
                existing.stream().filter(p -> p.getId().equals(photoDto.getId())).findFirst().ifPresent(p -> {
                    p.setPhotoUrl(photoDto.getPhotoUrl());
                    p.setLabel(photoDto.getLabel());
                    athletePhotoRepository.save(p);
                });
            }
        }
    }

    @Transactional(readOnly = true)
    public List<AthleteDto> findAll() {
        return toDtosWithPhotos(athleteRepository.findAll());
    }

    @Transactional(readOnly = true)
    public List<AthleteDto> search(String sport, String team, String nameContains) {
        List<Athlete> pool = sport != null ? athleteRepository.findBySport(sport) : athleteRepository.findAll();
        List<Athlete> filtered = pool.stream()
                .filter(a -> team == null || team.isBlank() || (a.getTeam() != null && a.getTeam().equalsIgnoreCase(team)))
                .filter(a -> nameContains == null || nameContains.isBlank()
                        || a.getName().toLowerCase().contains(nameContains.toLowerCase()))
                .collect(Collectors.toList());
        return toDtosWithPhotos(filtered);
    }

    @Transactional
    public AthleteDto create(AthleteDto dto) {
        Athlete athlete = new Athlete();
        athlete.setName(dto.getName());
        athlete.setSport(dto.getSport());
        athlete.setTeam(dto.getTeam());
        athlete.setPhotoUrl(dto.getPhotoUrl());
        athlete = athleteRepository.save(athlete);
        applyAdditionalPhotos(athlete, dto.getAdditionalPhotos());
        return toDtoWithPhotos(athlete);
    }

    @Transactional
    public AthleteDto update(Long id, AthleteDto dto) {
        Athlete athlete = athleteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No athlete found with id " + id));
        athlete.setName(dto.getName());
        athlete.setSport(dto.getSport());
        athlete.setTeam(dto.getTeam());
        athlete.setPhotoUrl(dto.getPhotoUrl());
        athlete = athleteRepository.save(athlete);
        applyAdditionalPhotos(athlete, dto.getAdditionalPhotos());
        return toDtoWithPhotos(athlete);
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
        athletePhotoRepository.deleteByAthlete_Id(id);
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
