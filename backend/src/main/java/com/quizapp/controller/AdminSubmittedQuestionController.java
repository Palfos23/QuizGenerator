package com.quizapp.controller;

import com.quizapp.dto.RejectSubmissionRequest;
import com.quizapp.dto.SubmittedQuestionDto;
import com.quizapp.service.SubmittedQuestionService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/question-submissions")
public class AdminSubmittedQuestionController {

    private final SubmittedQuestionService submittedQuestionService;

    public AdminSubmittedQuestionController(SubmittedQuestionService submittedQuestionService) {
        this.submittedQuestionService = submittedQuestionService;
    }

    @GetMapping
    public List<SubmittedQuestionDto> findAll() {
        return submittedQuestionService.listAll();
    }

    @PostMapping("/{id}/approve")
    public SubmittedQuestionDto approve(@PathVariable Long id) {
        return submittedQuestionService.approve(id);
    }

    @PostMapping("/{id}/reject")
    public SubmittedQuestionDto reject(@PathVariable Long id, @Valid @RequestBody RejectSubmissionRequest request) {
        return submittedQuestionService.reject(id, request);
    }
}
