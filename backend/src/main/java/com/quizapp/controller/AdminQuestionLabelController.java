package com.quizapp.controller;

import com.quizapp.dto.QuestionLabelDto;
import com.quizapp.dto.QuestionLabelRequest;
import com.quizapp.service.QuestionLabelService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/question-labels")
public class AdminQuestionLabelController {

    private final QuestionLabelService labelService;

    public AdminQuestionLabelController(QuestionLabelService labelService) {
        this.labelService = labelService;
    }

    @GetMapping
    public List<QuestionLabelDto> findAll() {
        return labelService.findAll();
    }

    @PostMapping
    public QuestionLabelDto create(@Valid @RequestBody QuestionLabelRequest request) {
        return labelService.create(request);
    }

    @PutMapping("/{id}")
    public QuestionLabelDto update(@PathVariable Long id, @Valid @RequestBody QuestionLabelRequest request) {
        return labelService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        labelService.delete(id);
    }
}
