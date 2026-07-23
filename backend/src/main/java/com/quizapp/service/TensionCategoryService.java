package com.quizapp.service;

import com.quizapp.dto.TensionCategoryDto;
import com.quizapp.exception.ResourceNotFoundException;
import com.quizapp.model.TensionAnswerCategory;
import com.quizapp.repository.TensionCategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TensionCategoryService {

    private final TensionCategoryRepository categoryRepository;

    public TensionCategoryService(TensionCategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public List<TensionCategoryDto> findAll() {
        return categoryRepository.findAll().stream().map(TensionCategoryService::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<String> getOptions(String categoryName) {
        return categoryRepository.findByNameIgnoreCase(categoryName)
                .map(TensionAnswerCategory::getOptions)
                .orElse(Collections.emptyList());
    }

    @Transactional
    public TensionCategoryDto create(TensionCategoryDto dto) {
        TensionAnswerCategory c = new TensionAnswerCategory();
        c.setName(dto.getName());
        c.setOptions(dto.getOptions());
        return toDto(categoryRepository.save(c));
    }

    @Transactional
    public TensionCategoryDto update(Long id, TensionCategoryDto dto) {
        TensionAnswerCategory c = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No tension category found with id " + id));
        c.setName(dto.getName());
        c.setOptions(dto.getOptions());
        return toDto(categoryRepository.save(c));
    }

    @Transactional
    public void delete(Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new ResourceNotFoundException("No tension category found with id " + id);
        }
        categoryRepository.deleteById(id);
    }

    static TensionCategoryDto toDto(TensionAnswerCategory c) {
        TensionCategoryDto dto = new TensionCategoryDto();
        dto.setId(c.getId());
        dto.setName(c.getName());
        dto.setOptions(c.getOptions());
        return dto;
    }
}
