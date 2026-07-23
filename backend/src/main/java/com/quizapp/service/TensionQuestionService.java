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
    public List<TensionQuestionDto> findAll() {
        return questionRepository.findAll().stream().map(TensionQuestionService::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TensionQuestionDto getOne(Long id) {
        TensionQuestion q = questionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No tension question found with id " + id));
        return toDto(q);
    }

    @Transactional(readOnly = true)
    public List<TensionQuestionDto> getRandom(int count, String mainCategory) {
        List<TensionQuestion> pool = (mainCategory == null || mainCategory.isBlank())
                ? questionRepository.findAll()
                : questionRepository.findByMainCategoryIgnoreCase(mainCategory);
        List<TensionQuestion> shuffled = new ArrayList<>(pool);
        Collections.shuffle(shuffled);
        return shuffled.stream().limit(count).map(TensionQuestionService::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<String> getDistinctMainCategories() {
        return questionRepository.findAll().stream()
                .map(TensionQuestion::getMainCategory)
                .filter(c -> c != null && !c.isBlank())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
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
