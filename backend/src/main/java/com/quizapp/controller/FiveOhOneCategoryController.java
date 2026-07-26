package com.quizapp.controller;

import com.quizapp.dto.FiveOhOneCategoryDto;
import com.quizapp.dto.FiveOhOneCategorySummaryDto;
import com.quizapp.service.FiveOhOneCategoryService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/501/categories")
public class FiveOhOneCategoryController {

    private final FiveOhOneCategoryService categoryService;

    public FiveOhOneCategoryController(FiveOhOneCategoryService categoryService) {
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
}
