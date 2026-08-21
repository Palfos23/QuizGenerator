package com.quizapp.controller;

import com.quizapp.dto.FiveOhOneCategoryDto;
import com.quizapp.dto.FiveOhOneCategorySummaryDto;
import com.quizapp.service.FiveOhOneCategoryService;
import com.quizapp.service.PlayAccessService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/501/categories")
public class FiveOhOneCategoryController {

    private final FiveOhOneCategoryService categoryService;
    private final PlayAccessService playAccessService;

    public FiveOhOneCategoryController(FiveOhOneCategoryService categoryService, PlayAccessService playAccessService) {
        this.categoryService = categoryService;
        this.playAccessService = playAccessService;
    }

    @GetMapping
    public List<FiveOhOneCategorySummaryDto> findAll() {
        return categoryService.findAllSummaries();
    }

    @GetMapping("/{id}")
    public FiveOhOneCategoryDto getOne(@PathVariable Long id, Authentication authentication) {
        playAccessService.requireFiveOhOneAccess(authentication);
        return categoryService.getOne(id);
    }
}
