package com.quizapp.controller;

import com.quizapp.dto.QuizDto;
import com.quizapp.dto.QuizTemplateSummaryDto;
import com.quizapp.service.QuizTemplateService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/quiz-templates")
public class QuizTemplateController {

    private final QuizTemplateService quizTemplateService;

    public QuizTemplateController(QuizTemplateService quizTemplateService) {
        this.quizTemplateService = quizTemplateService;
    }

    @GetMapping
    public List<QuizTemplateSummaryDto> findAll() {
        return quizTemplateService.findAll();
    }

    @PostMapping("/{id}/copy")
    public QuizDto copy(@PathVariable Long id, Authentication authentication) {
        return quizTemplateService.copyToMyQuizzes(id, authentication.getName());
    }
}
