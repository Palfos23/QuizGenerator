package com.quizapp.controller;

import com.quizapp.dto.WeeklyQuizAttemptDetailDto;
import com.quizapp.dto.WeeklyQuizPendingAttemptDto;
import com.quizapp.dto.WeeklyQuizResolveRequest;
import com.quizapp.service.WeeklyQuizReviewService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/weekly-quiz")
public class AdminWeeklyQuizController {

    private final WeeklyQuizReviewService weeklyQuizReviewService;

    public AdminWeeklyQuizController(WeeklyQuizReviewService weeklyQuizReviewService) {
        this.weeklyQuizReviewService = weeklyQuizReviewService;
    }

    @GetMapping("/pending-attempts")
    public List<WeeklyQuizPendingAttemptDto> pendingAttempts() {
        return weeklyQuizReviewService.listPendingAttempts();
    }

    @GetMapping("/attempts/{id}")
    public WeeklyQuizAttemptDetailDto attemptDetail(@PathVariable Long id) {
        return weeklyQuizReviewService.getAttemptDetail(id);
    }

    @PostMapping("/answers/{id}/resolve")
    public void resolve(@PathVariable Long id, @Valid @RequestBody WeeklyQuizResolveRequest request) {
        weeklyQuizReviewService.resolve(id, request.getCorrect());
    }
}
