package com.quizapp.controller;

import com.quizapp.dto.DailyQuizPlayStateDto;
import com.quizapp.dto.DailyQuizScoreboardDto;
import com.quizapp.dto.DailyQuizSetSummaryDto;
import com.quizapp.dto.DailyQuizSubmitRequest;
import com.quizapp.service.DailyQuizService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/daily-quiz")
public class DailyQuizController {

    private final DailyQuizService dailyQuizService;

    public DailyQuizController(DailyQuizService dailyQuizService) {
        this.dailyQuizService = dailyQuizService;
    }

    @GetMapping("/active")
    public List<DailyQuizSetSummaryDto> active(Authentication authentication) {
        return dailyQuizService.findActive(authentication.getName());
    }

    @GetMapping("/archive")
    public List<DailyQuizSetSummaryDto> archive(Authentication authentication) {
        return dailyQuizService.findArchive(authentication.getName());
    }

    @GetMapping("/{id}/play")
    public DailyQuizPlayStateDto play(@PathVariable Long id, Authentication authentication) {
        return dailyQuizService.getPlayState(id, authentication.getName());
    }

    @PostMapping("/{id}/submit")
    public DailyQuizPlayStateDto submit(@PathVariable Long id, @Valid @RequestBody DailyQuizSubmitRequest request, Authentication authentication) {
        return dailyQuizService.submitAnswers(id, authentication.getName(), request);
    }

    @GetMapping("/{id}/scoreboard")
    public DailyQuizScoreboardDto scoreboard(@PathVariable Long id, Authentication authentication) {
        return dailyQuizService.getScoreboard(id, authentication.getName());
    }

    @PutMapping("/{id}/leaderboard-preference")
    public void setLeaderboardPreference(@PathVariable Long id, @RequestParam boolean include, Authentication authentication) {
        dailyQuizService.setLeaderboardPreference(id, authentication.getName(), include);
    }
}
