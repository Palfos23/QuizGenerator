package com.quizapp.service;

import com.quizapp.dto.AthleteDto;
import com.quizapp.dto.BullseyeEntryInputDto;
import com.quizapp.dto.BullseyeQuestionAdminDetailDto;
import com.quizapp.dto.BullseyeQuestionRequest;
import com.quizapp.dto.BullseyeQuestionSummaryDto;
import com.quizapp.exception.ResourceNotFoundException;
import com.quizapp.model.Athlete;
import com.quizapp.model.BullseyeEntry;
import com.quizapp.model.BullseyeQuestion;
import com.quizapp.repository.AthleteRepository;
import com.quizapp.repository.BullseyeQuestionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class BullseyeAdminService {

    private final BullseyeQuestionRepository bullseyeQuestionRepository;
    private final AthleteRepository athleteRepository;
    private final AthleteService athleteService;

    public BullseyeAdminService(BullseyeQuestionRepository bullseyeQuestionRepository,
                                 AthleteRepository athleteRepository, AthleteService athleteService) {
        this.bullseyeQuestionRepository = bullseyeQuestionRepository;
        this.athleteRepository = athleteRepository;
        this.athleteService = athleteService;
    }

    @Transactional(readOnly = true)
    public List<BullseyeQuestionSummaryDto> findAll() {
        return bullseyeQuestionRepository.findAllSummaries().stream()
                .sorted((a, b) -> a.getTitle().compareToIgnoreCase(b.getTitle()))
                .map(row -> new BullseyeQuestionSummaryDto(row.getId(), row.getTitle(), row.getSport(),
                        row.getTargetValue(), row.getStatLabel(), row.getEntryCount().intValue(),
                        row.getExcludedFromBullseye(), row.getEntireCategoryPool()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BullseyeQuestionAdminDetailDto getOne(Long id) {
        BullseyeQuestion question = bullseyeQuestionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No question found with id " + id));
        return toDetailDto(question);
    }

    @Transactional
    public BullseyeQuestionAdminDetailDto create(BullseyeQuestionRequest request) {
        BullseyeQuestion question = new BullseyeQuestion();
        applyRequest(question, request);
        return toDetailDto(bullseyeQuestionRepository.save(question));
    }

    @Transactional
    public BullseyeQuestionAdminDetailDto update(Long id, BullseyeQuestionRequest request) {
        BullseyeQuestion question = bullseyeQuestionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No question found with id " + id));
        applyRequest(question, request);
        return toDetailDto(bullseyeQuestionRepository.save(question));
    }

    @Transactional
    public void delete(Long id) {
        if (!bullseyeQuestionRepository.existsById(id)) {
            throw new ResourceNotFoundException("No question found with id " + id);
        }
        bullseyeQuestionRepository.deleteById(id);
    }

    private void applyRequest(BullseyeQuestion question, BullseyeQuestionRequest request) {
        question.setTitle(request.getTitle());
        question.setSport(request.getSport());
        question.setTargetValue(request.getTargetValue());
        question.setStatLabel(request.getStatLabel());
        question.setUpdatedAt(java.time.Instant.now());
        question.setExcludedFromBullseye(request.isExcludedFromBullseye());
        question.setEntireCategoryPool(request.isEntireCategoryPool());

        if (request.getEntries().size() < 2) {
            throw new IllegalArgumentException("Add at least 2 athletes with stat values.");
        }

        // Match against existing entries by athlete so a routine stat-value edit
        // preserves each entry's id - not load-bearing today (Bullseye has no
        // persisted per-user attempt referencing entry ids the way GridAttempt
        // does), but keeps the same safe habit as GridAdminService.applyRequest
        // in case that ever changes.
        Map<Long, BullseyeEntry> existingByAthleteId = question.getEntries().stream()
                .collect(Collectors.toMap(e -> e.getAthlete().getId(), e -> e, (a, b) -> a));

        List<Long> athleteIds = request.getEntries().stream()
                .map(BullseyeEntryInputDto::getAthleteId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, Athlete> athleteById = athleteRepository.findAllById(athleteIds).stream()
                .collect(Collectors.toMap(Athlete::getId, a -> a));
        if (athleteById.size() != athleteIds.size()) {
            List<Long> missing = athleteIds.stream().filter(id -> !athleteById.containsKey(id)).collect(Collectors.toList());
            throw new IllegalArgumentException("No athlete found with id(s) " + missing);
        }

        int index = 0;
        Set<BullseyeEntry> entries = new LinkedHashSet<>();
        for (BullseyeEntryInputDto input : request.getEntries()) {
            Athlete athlete = athleteById.get(input.getAthleteId());
            if (input.getStatValue() == null) {
                throw new IllegalArgumentException("'" + athlete.getName() + "' is missing a stat value.");
            }
            BullseyeEntry entry = existingByAthleteId.getOrDefault(input.getAthleteId(), new BullseyeEntry());
            entry.setAthlete(athlete);
            entry.setStatValue(input.getStatValue());
            entry.setOrderIndex(index++);
            entries.add(entry);
        }
        question.setEntries(entries);
    }

    private BullseyeQuestionAdminDetailDto toDetailDto(BullseyeQuestion question) {
        BullseyeQuestionAdminDetailDto dto = new BullseyeQuestionAdminDetailDto();
        dto.setId(question.getId());
        dto.setTitle(question.getTitle());
        dto.setSport(question.getSport());
        dto.setTargetValue(question.getTargetValue());
        dto.setStatLabel(question.getStatLabel());
        dto.setExcludedFromBullseye(question.isExcludedFromBullseye());
        dto.setEntireCategoryPool(question.isEntireCategoryPool());

        List<Athlete> distinctAthletes = question.getEntries().stream()
                .map(BullseyeEntry::getAthlete)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, AthleteDto> athleteDtoById = athleteService.toDtosWithPhotos(distinctAthletes).stream()
                .collect(Collectors.toMap(AthleteDto::getId, a -> a));

        dto.setEntries(question.getEntries().stream()
                .sorted((a, b) -> Integer.compare(a.getOrderIndex(), b.getOrderIndex()))
                .map(e -> new BullseyeQuestionAdminDetailDto.EntryDetail(
                        e.getId(), athleteDtoById.get(e.getAthlete().getId()), e.getStatValue()))
                .collect(Collectors.toList()));
        return dto;
    }
}
