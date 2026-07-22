package com.quizapp.controller;

import com.quizapp.dto.CategoryStatDto;
import com.quizapp.dto.ImportResultDto;
import com.quizapp.dto.QuestionDto;
import com.quizapp.service.QuestionImportService;
import com.quizapp.service.QuestionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Admin-only endpoints for managing the question bank. Every route here requires
 * a valid ADMIN-role JWT - enforced in SecurityConfig, not in this class.
 */
@RestController
@RequestMapping("/api/admin/questions")
public class AdminQuestionController {

    private final QuestionService questionService;
    private final QuestionImportService questionImportService;

    public AdminQuestionController(QuestionService questionService, QuestionImportService questionImportService) {
        this.questionService = questionService;
        this.questionImportService = questionImportService;
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

    /** Coverage breakdown (count + difficulty range per category/language) for the "bank health" view. */
    @GetMapping("/stats")
    public List<CategoryStatDto> getStats() {
        return questionService.getStats();
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

    /** Bulk-add questions from a CSV upload. Invalid rows are skipped and reported, not fatal to the whole import. */
    @PostMapping("/import")
    public ImportResultDto importCsv(@RequestParam("file") MultipartFile file) {
        return questionImportService.importCsv(file);
    }

    /** One-click on-ramp for a brand-new, empty question bank. */
    @PostMapping("/starter-pack")
    public com.quizapp.dto.StarterPackResultDto addStarterPack() {
        return questionService.addStarterPack();
    }
}
