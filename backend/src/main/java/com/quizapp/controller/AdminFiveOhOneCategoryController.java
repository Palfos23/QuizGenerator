package com.quizapp.controller;

import com.quizapp.dto.FiveOhOneCategoryDto;
import com.quizapp.dto.FiveOhOneCategoryRequest;
import com.quizapp.dto.FiveOhOneCategorySummaryDto;
import com.quizapp.service.FiveOhOneCategoryService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/501/categories")
public class AdminFiveOhOneCategoryController {

    private final FiveOhOneCategoryService categoryService;

    public AdminFiveOhOneCategoryController(FiveOhOneCategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public List<FiveOhOneCategorySummaryDto> findAll() {
        return categoryService.findAllSummaries();
    }

    @GetMapping("/{id}")
    public FiveOhOneCategoryDto getOne(@PathVariable Long id) {
        return categoryService.getOne(id);
    }

    @PostMapping
    public FiveOhOneCategoryDto create(@Valid @RequestBody FiveOhOneCategoryRequest request) {
        return categoryService.create(request);
    }

    @PutMapping("/{id}")
    public FiveOhOneCategoryDto update(@PathVariable Long id, @Valid @RequestBody FiveOhOneCategoryRequest request) {
        return categoryService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        categoryService.delete(id);
    }
}
