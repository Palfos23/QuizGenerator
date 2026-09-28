package com.quizapp.controller;

import com.quizapp.dto.WeeklyQuizPlayStateDto;
import com.quizapp.dto.WeeklyQuizSubmitRequest;
import com.quizapp.service.WeeklyQuizService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/weekly-quiz")
public class WeeklyQuizController {

    private final WeeklyQuizService weeklyQuizService;

    public WeeklyQuizController(WeeklyQuizService weeklyQuizService) {
        this.weeklyQuizService = weeklyQuizService;
    }

    @GetMapping("/play")
    public WeeklyQuizPlayStateDto play(Authentication authentication) {
        return weeklyQuizService.getPlayState(authentication.getName());
    }

    @PostMapping("/submit")
    public WeeklyQuizPlayStateDto submit(@Valid @RequestBody WeeklyQuizSubmitRequest request, Authentication authentication) {
        return weeklyQuizService.submitAnswers(authentication.getName(), request);
    }
}
