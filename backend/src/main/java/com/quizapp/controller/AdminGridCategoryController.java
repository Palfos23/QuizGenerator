package com.quizapp.controller;

import com.quizapp.dto.GridCategoryDto;
import com.quizapp.dto.GridCategoryRequest;
import com.quizapp.service.GridCategoryService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/grid-categories")
public class AdminGridCategoryController {

    private final GridCategoryService categoryService;

    public AdminGridCategoryController(GridCategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public List<GridCategoryDto> findAll() {
        return categoryService.findAll();
    }

    @PostMapping
    public GridCategoryDto create(@Valid @RequestBody GridCategoryRequest request) {
        return categoryService.create(request);
    }

    @PutMapping("/{id}")
    public GridCategoryDto update(@PathVariable Long id, @Valid @RequestBody GridCategoryRequest request) {
        return categoryService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        categoryService.delete(id);
    }
}
