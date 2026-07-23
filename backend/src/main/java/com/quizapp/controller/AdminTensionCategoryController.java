package com.quizapp.controller;

import com.quizapp.dto.TensionCategoryDto;
import com.quizapp.service.TensionCategoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/tension/categories")
public class AdminTensionCategoryController {

    private final TensionCategoryService categoryService;

    public AdminTensionCategoryController(TensionCategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public List<TensionCategoryDto> findAll() {
        return categoryService.findAll();
    }

    @PostMapping
    public ResponseEntity<TensionCategoryDto> create(@Valid @RequestBody TensionCategoryDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryService.create(dto));
    }

    @PutMapping("/{id}")
    public TensionCategoryDto update(@PathVariable Long id, @Valid @RequestBody TensionCategoryDto dto) {
        return categoryService.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
