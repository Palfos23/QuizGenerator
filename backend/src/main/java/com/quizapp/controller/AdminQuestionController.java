package com.quizapp.controller;

import com.quizapp.dto.QuestionDto;
import com.quizapp.service.QuestionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin-only endpoints for managing the question bank.
 * NOTE: this scaffold does not include authentication. Before going live,
 * put these routes behind Spring Security (e.g. an ADMIN role) - see README.
 */
@RestController
@RequestMapping("/api/admin/questions")
public class AdminQuestionController {

    private final QuestionService questionService;

    public AdminQuestionController(QuestionService questionService) {
        this.questionService = questionService;
    }

    @GetMapping
    public List<QuestionDto> getAll() {
        return questionService.findAll();
    }

    @GetMapping("/{id}")
    public QuestionDto getOne(@PathVariable Long id) {
        return questionService.findById(id);
    }

    @GetMapping("/categories")
    public List<String> getCategories() {
        return questionService.findAllCategories();
    }

    @PostMapping
    public ResponseEntity<QuestionDto> create(@Valid @RequestBody QuestionDto dto) {
        QuestionDto created = questionService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public QuestionDto update(@PathVariable Long id, @Valid @RequestBody QuestionDto dto) {
        return questionService.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        questionService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
