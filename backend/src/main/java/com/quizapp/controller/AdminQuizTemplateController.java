package com.quizapp.controller;

import com.quizapp.dto.QuizDto;
import com.quizapp.dto.QuizTemplateSummaryDto;
import com.quizapp.service.QuizTemplateService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/quiz-templates")
public class AdminQuizTemplateController {

    private final QuizTemplateService quizTemplateService;

    public AdminQuizTemplateController(QuizTemplateService quizTemplateService) {
        this.quizTemplateService = quizTemplateService;
    }

    @GetMapping
    public List<QuizTemplateSummaryDto> findAll() {
        return quizTemplateService.findAll();
    }

    @GetMapping("/{id}")
    public QuizDto getOne(@PathVariable Long id) {
        return quizTemplateService.getOne(id);
    }

    @PostMapping
    public ResponseEntity<QuizDto> create(@Valid @RequestBody QuizDto quiz) {
        return ResponseEntity.status(HttpStatus.CREATED).body(quizTemplateService.create(quiz));
    }

    @PutMapping("/{id}")
    public QuizDto update(@PathVariable Long id, @Valid @RequestBody QuizDto quiz) {
        return quizTemplateService.update(id, quiz);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        quizTemplateService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
