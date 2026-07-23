package com.quizapp.controller;

import com.quizapp.dto.SubmittedQuestionDto;
import com.quizapp.service.SubmittedQuestionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/questions/submissions")
public class SubmittedQuestionController {

    private final SubmittedQuestionService submittedQuestionService;

    public SubmittedQuestionController(SubmittedQuestionService submittedQuestionService) {
        this.submittedQuestionService = submittedQuestionService;
    }

    @PostMapping
    public ResponseEntity<SubmittedQuestionDto> submit(@Valid @RequestBody SubmittedQuestionDto dto, Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(submittedQuestionService.submit(authentication.getName(), dto));
    }

    @GetMapping("/mine")
    public List<SubmittedQuestionDto> mine(Authentication authentication) {
        return submittedQuestionService.listMine(authentication.getName());
    }
}
