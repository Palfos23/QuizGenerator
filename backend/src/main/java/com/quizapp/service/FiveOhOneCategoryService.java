package com.quizapp.service;

import com.quizapp.dto.*;
import com.quizapp.exception.ResourceNotFoundException;
import com.quizapp.model.FiveOhOneCategory;
import com.quizapp.model.FiveOhOneEntry;
import com.quizapp.repository.FiveOhOneCategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class FiveOhOneCategoryService {

    private final FiveOhOneCategoryRepository categoryRepository;

    public FiveOhOneCategoryService(FiveOhOneCategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public List<FiveOhOneCategorySummaryDto> findAllSummaries() {
        return categoryRepository.findAll().stream()
                .map(c -> new FiveOhOneCategorySummaryDto(c.getId(), c.getTitle(), c.getDescription(), c.getEntries().size()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public FiveOhOneCategoryDto getOne(Long id) {
        return toDto(categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No 501 category found with id " + id)));
    }

    @Transactional
    public FiveOhOneCategoryDto create(FiveOhOneCategoryRequest request) {
        FiveOhOneCategory category = new FiveOhOneCategory();
        applyRequest(category, request);
        return toDto(categoryRepository.save(category));
    }

    @Transactional
    public FiveOhOneCategoryDto update(Long id, FiveOhOneCategoryRequest request) {
        FiveOhOneCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No 501 category found with id " + id));
        applyRequest(category, request);
        return toDto(categoryRepository.save(category));
    }

    @Transactional
    public void delete(Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new ResourceNotFoundException("No 501 category found with id " + id);
        }
        categoryRepository.deleteById(id);
    }

    private void applyRequest(FiveOhOneCategory category, FiveOhOneCategoryRequest request) {
        category.setTitle(request.getTitle());
        category.setDescription(request.getDescription());
        category.setUpdatedAt(java.time.Instant.now());

        // Reuse existing entries by name where possible, rather than always creating
        // fresh rows - keeps saves fast for large categories and avoids needlessly
        // recreating rows that didn't actually change.
        Map<String, FiveOhOneEntry> existingByName = category.getEntries().stream()
                .collect(Collectors.toMap(e -> e.getName().toLowerCase(), e -> e, (a, b) -> a));

        List<FiveOhOneEntry> entries = request.getEntries().stream()
                .map(input -> {
                    FiveOhOneEntry entry = existingByName.getOrDefault(input.getName().toLowerCase(), new FiveOhOneEntry());
                    entry.setName(input.getName().trim());
                    entry.setValue(input.getValue());
                    return entry;
                })
                .collect(Collectors.toList());
        category.setEntries(entries);
    }

    static FiveOhOneCategoryDto toDto(FiveOhOneCategory c) {
        List<FiveOhOneEntryDto> entries = c.getEntries().stream()
                .map(e -> new FiveOhOneEntryDto(e.getId(), e.getName(), e.getValue()))
                .collect(Collectors.toList());
        FiveOhOneCategoryDto dto = new FiveOhOneCategoryDto(c.getId(), c.getTitle(), c.getDescription(), entries);
        dto.setUpdatedAt(c.getUpdatedAt());
        return dto;
    }
}
