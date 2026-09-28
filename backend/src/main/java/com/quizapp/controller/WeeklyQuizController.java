package com.quizapp.controller;

import com.quizapp.dto.WeeklyQuizPlayStateDto;
import com.quizapp.dto.WeeklyQuizScoreboardDto;
import com.quizapp.dto.WeeklyQuizSetSummaryDto;
import com.quizapp.dto.WeeklyQuizSubmitRequest;
import com.quizapp.service.WeeklyQuizService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/weekly-quiz")
public class WeeklyQuizController {

    private final WeeklyQuizService weeklyQuizService;

    public WeeklyQuizController(WeeklyQuizService weeklyQuizService) {
        this.weeklyQuizService = weeklyQuizService;
    }

    @GetMapping("/active")
    public List<WeeklyQuizSetSummaryDto> active(Authentication authentication) {
        return weeklyQuizService.findActive(authentication.getName());
    }

    @GetMapping("/archive")
    public List<WeeklyQuizSetSummaryDto> archive(Authentication authentication) {
        return weeklyQuizService.findArchive(authentication.getName());
    }

    @GetMapping("/{id}/play")
    public WeeklyQuizPlayStateDto play(@PathVariable Long id, Authentication authentication) {
        return weeklyQuizService.getPlayState(id, authentication.getName());
    }

    @PostMapping("/{id}/submit")
    public WeeklyQuizPlayStateDto submit(@PathVariable Long id, @Valid @RequestBody WeeklyQuizSubmitRequest request, Authentication authentication) {
        return weeklyQuizService.submitAnswers(id, authentication.getName(), request);
    }

    @GetMapping("/{id}/scoreboard")
    public WeeklyQuizScoreboardDto scoreboard(@PathVariable Long id, Authentication authentication) {
        return weeklyQuizService.getScoreboard(id, authentication.getName());
    }

    @PutMapping("/{id}/leaderboard-preference")
    public void setLeaderboardPreference(@PathVariable Long id, @RequestParam boolean include, Authentication authentication) {
        weeklyQuizService.setLeaderboardPreference(id, authentication.getName(), include);
    }
}
