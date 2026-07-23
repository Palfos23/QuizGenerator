package com.quizapp.controller;

import com.quizapp.dto.TensionQuestionDto;
import com.quizapp.service.TensionQuestionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/tension/questions")
public class AdminTensionQuestionController {

    private final TensionQuestionService questionService;

    public AdminTensionQuestionController(TensionQuestionService questionService) {
        this.questionService = questionService;
    }

    @GetMapping
    public List<TensionQuestionDto> findAll() {
        return questionService.findAll();
    }

    @GetMapping("/{id}")
    public TensionQuestionDto getOne(@PathVariable Long id) {
        return questionService.getOne(id);
    }

    @PostMapping
    public ResponseEntity<TensionQuestionDto> create(@Valid @RequestBody TensionQuestionDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(questionService.create(dto));
    }

    @PutMapping("/{id}")
    public TensionQuestionDto update(@PathVariable Long id, @Valid @RequestBody TensionQuestionDto dto) {
        return questionService.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        questionService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
