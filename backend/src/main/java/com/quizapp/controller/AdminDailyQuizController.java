package com.quizapp.controller;

import com.quizapp.dto.DailyQuizAttemptDetailDto;
import com.quizapp.dto.DailyQuizDayAttemptsDto;
import com.quizapp.dto.DailyQuizPendingAttemptDto;
import com.quizapp.dto.DailyQuizResolveRequest;
import com.quizapp.service.DailyQuizReviewService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/daily-quiz")
public class AdminDailyQuizController {

    private final DailyQuizReviewService dailyQuizReviewService;

    public AdminDailyQuizController(DailyQuizReviewService dailyQuizReviewService) {
        this.dailyQuizReviewService = dailyQuizReviewService;
    }

    @GetMapping("/pending-attempts")
    public List<DailyQuizPendingAttemptDto> pendingAttempts() {
        return dailyQuizReviewService.listPendingAttempts();
    }

    @GetMapping("/sets/{id}/attempts")
    public DailyQuizDayAttemptsDto attemptsForSet(@PathVariable Long id) {
        return dailyQuizReviewService.listAttemptsForSet(id);
    }

    @GetMapping("/sets/{id}/questions")
    public List<com.quizapp.dto.QuestionDto> questionsForSet(@PathVariable Long id) {
        return dailyQuizReviewService.listQuestionsForSet(id);
    }

    @GetMapping("/attempts/{id}")
    public DailyQuizAttemptDetailDto attemptDetail(@PathVariable Long id) {
        return dailyQuizReviewService.getAttemptDetail(id);
    }

    @PostMapping("/answers/{id}/resolve")
    public void resolve(@PathVariable Long id, @Valid @RequestBody DailyQuizResolveRequest request) {
        dailyQuizReviewService.resolve(id, request.getCorrect());
    }
}
