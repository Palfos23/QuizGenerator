package com.quizapp.service;

import com.quizapp.dto.TensionAnswerEntryDto;
import com.quizapp.dto.TensionQuestionDto;
import com.quizapp.exception.ResourceNotFoundException;
import com.quizapp.model.TensionAnswerEntry;
import com.quizapp.model.TensionQuestion;
import com.quizapp.repository.TensionQuestionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TensionQuestionService {

    private final TensionQuestionRepository questionRepository;

    public TensionQuestionService(TensionQuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
    }

    @Transactional(readOnly = true)
    public List<com.quizapp.dto.TensionQuestionSummaryDto> findAll() {
        return questionRepository.findAllSummaries().stream()
                .map(p -> new com.quizapp.dto.TensionQuestionSummaryDto(
                        p.getId(), p.getTitle(), p.getMainCategory(), p.getAnswersCategory(),
                        p.getSource(), p.getSafeCount(), p.getTensionCount()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TensionQuestionDto getOne(Long id) {
        TensionQuestion q = questionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No tension question found with id " + id));
        return toDto(q);
    }

    @Transactional(readOnly = true)
    public List<TensionQuestionDto> getRandom(int count, String mainCategory, List<String> excludeCategories) {
        return loadRandomSubset(candidateIds(mainCategory, excludeCategories), count);
    }

    // For the "pick your question" round-start screen: a small pool of
    // candidates for this specific round, excluding whatever's already been
    // played this game so a repeat never gets offered.
    @Transactional(readOnly = true)
    public List<TensionQuestionDto> getRoundChoices(int count, String mainCategory, List<String> excludeCategories, List<Long> excludeIds) {
        List<Long> ids = candidateIds(mainCategory, excludeCategories);
        List<Long> available = ids.stream()
                .filter(id -> excludeIds == null || !excludeIds.contains(id))
                .collect(Collectors.toList());
        return loadRandomSubset(available, count);
    }

    // A specific category (if set) takes priority - excluding categories only
    // makes sense for the "draw from everything" case, not "just this one".
    private List<Long> candidateIds(String mainCategory, List<String> excludeCategories) {
        if (mainCategory != null && !mainCategory.isBlank()) {
            return questionRepository.findIdsByMainCategoryIgnoreCase(mainCategory);
        }
        if (excludeCategories != null && !excludeCategories.isEmpty()) {
            List<String> excludedLower = excludeCategories.stream()
                    .map(String::toLowerCase)
                    .collect(Collectors.toList());
            return questionRepository.findIdsByMainCategoryNotIn(excludedLower);
        }
        return questionRepository.findAllIds();
    }

    // Shuffling and sampling at the ID level is cheap regardless of how many
    // questions exist in the category - only the small sampled subset actually
    // gets its full entity (and answer lists) loaded.
    private List<TensionQuestionDto> loadRandomSubset(List<Long> ids, int count) {
        List<Long> shuffled = new ArrayList<>(ids);
        Collections.shuffle(shuffled);
        List<Long> sampled = shuffled.stream().limit(count).collect(Collectors.toList());
        return questionRepository.findAllById(sampled).stream()
                .map(TensionQuestionService::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<String> getDistinctMainCategories() {
        return questionRepository.findDistinctMainCategories();
    }

    @Transactional
    public TensionQuestionDto create(TensionQuestionDto dto) {
        TensionQuestion q = new TensionQuestion();
        applyDto(q, dto);
        return toDto(questionRepository.save(q));
    }

    @Transactional
    public TensionQuestionDto update(Long id, TensionQuestionDto dto) {
        TensionQuestion q = questionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No tension question found with id " + id));
        applyDto(q, dto);
        return toDto(questionRepository.save(q));
    }

    @Transactional
    public void delete(Long id) {
        if (!questionRepository.existsById(id)) {
            throw new ResourceNotFoundException("No tension question found with id " + id);
        }
        questionRepository.deleteById(id);
    }

    private void applyDto(TensionQuestion q, TensionQuestionDto dto) {
        q.setTitle(dto.getTitle());
        q.setMainCategory(dto.getMainCategory());
        q.setAnswersCategory(dto.getAnswersCategory());
        q.setSource(dto.getSource());
        q.setSafeAnswers(toEntryEntities(dto.getSafeAnswers()));
        q.setTensionAnswers(toEntryEntities(dto.getTensionAnswers()));
    }

    private List<TensionAnswerEntry> toEntryEntities(List<TensionAnswerEntryDto> dtos) {
        if (dtos == null) return new ArrayList<>();
        return dtos.stream().map(d -> {
            TensionAnswerEntry e = new TensionAnswerEntry();
            e.setRank(d.getRank());
            e.setText(d.getText());
            return e;
        }).collect(Collectors.toList());
    }

    static TensionQuestionDto toDto(TensionQuestion q) {
        TensionQuestionDto dto = new TensionQuestionDto();
        dto.setId(q.getId());
        dto.setTitle(q.getTitle());
        dto.setMainCategory(q.getMainCategory());
        dto.setAnswersCategory(q.getAnswersCategory());
        dto.setSource(q.getSource());
        dto.setSafeAnswers(q.getSafeAnswers().stream()
                .sorted((a, b) -> a.getRank() - b.getRank())
                .map(TensionQuestionService::toEntryDto)
                .collect(Collectors.toList()));
        dto.setTensionAnswers(q.getTensionAnswers().stream()
                .sorted((a, b) -> a.getRank() - b.getRank())
                .map(TensionQuestionService::toEntryDto)
                .collect(Collectors.toList()));
        return dto;
    }

    private static TensionAnswerEntryDto toEntryDto(TensionAnswerEntry e) {
        TensionAnswerEntryDto dto = new TensionAnswerEntryDto();
        dto.setId(e.getId());
        dto.setRank(e.getRank());
        dto.setText(e.getText());
        return dto;
    }
}
