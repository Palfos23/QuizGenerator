package com.quizapp.service;

import com.quizapp.dto.QuestionDto;
import com.quizapp.exception.ResourceNotFoundException;
import com.quizapp.model.Question;
import com.quizapp.repository.QuestionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class QuestionService {

    private final QuestionRepository questionRepository;

    public QuestionService(QuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
    }

    @Transactional(readOnly = true)
    public List<QuestionDto> findAll() {
        return questionRepository.findAll().stream()
                .map(QuestionMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public QuestionDto findById(Long id) {
        Question question = questionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No question found with id " + id));
        return QuestionMapper.toDto(question);
    }

    @Transactional(readOnly = true)
    public List<String> findAllCategories() {
        return questionRepository.findAllDistinctCategories();
    }

    @Transactional(readOnly = true)
    public List<String> findCategoriesForLanguage(com.quizapp.model.Language language) {
        return questionRepository.findByLanguage(language).stream()
                .map(Question::getCategory)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    @Transactional
    public QuestionDto create(QuestionDto dto) {
        Question saved = questionRepository.save(QuestionMapper.toEntity(dto));
        return QuestionMapper.toDto(saved);
    }

    @Transactional
    public QuestionDto update(Long id, QuestionDto dto) {
        Question existing = questionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No question found with id " + id));

        existing.setQuestionText(dto.getQuestionText());
        existing.setCategory(dto.getCategory());
        existing.setDifficultyLevel(dto.getDifficultyLevel());
        existing.setLanguage(dto.getLanguage());
        existing.setAnswer(dto.getAnswer());
        existing.setCouldChange(dto.isCouldChange());

        Question saved = questionRepository.save(existing);
        return QuestionMapper.toDto(saved);
    }

    @Transactional
    public void delete(Long id) {
        if (!questionRepository.existsById(id)) {
            throw new ResourceNotFoundException("No question found with id " + id);
        }
        questionRepository.deleteById(id);
    }
}
