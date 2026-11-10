package com.quizapp.controller;

import com.quizapp.dto.DailyQuizAttemptDetailDto;
import com.quizapp.dto.DailyQuizDayAttemptsDto;
import com.quizapp.dto.DailyQuizPendingAttemptDto;
import com.quizapp.dto.DailyQuizResolveRequest;
import com.quizapp.dto.DailyQuizReviewDto;
import com.quizapp.service.DailyQuizComplaintService;
import com.quizapp.service.DailyQuizReviewService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/daily-quiz")
public class AdminDailyQuizController {

    private final DailyQuizReviewService dailyQuizReviewService;
    private final DailyQuizComplaintService complaintService;

    public AdminDailyQuizController(DailyQuizReviewService dailyQuizReviewService, DailyQuizComplaintService complaintService) {
        this.dailyQuizReviewService = dailyQuizReviewService;
        this.complaintService = complaintService;
    }

    // ---- per-answer review: each distinct answer once, decided for everybody who wrote it ----

    @GetMapping("/answer-groups")
    public DailyQuizReviewDto.AnswerGroups answerGroups() {
        return dailyQuizReviewService.listAnswerGroups();
    }

    @PostMapping("/answer-groups/decide")
    public DailyQuizReviewDto.Changed decideGroup(@RequestBody DailyQuizReviewDto.DecideGroupRequest request) {
        return new DailyQuizReviewDto.Changed(dailyQuizReviewService.decideGroup(
                request.questionId(), request.text(), request.correct(), request.remember()));
    }

    @PostMapping("/answer-groups/accept-typos")
    public DailyQuizReviewDto.Changed acceptTypos(@RequestBody DailyQuizReviewDto.AcceptTyposRequest request) {
        return new DailyQuizReviewDto.Changed(dailyQuizReviewService.acceptLikelyTypos(request.setId()));
    }

    // ---- complaints ("be om ny vurdering") ----

    @GetMapping("/complaints")
    public List<DailyQuizReviewDto.Complaint> complaints() {
        return complaintService.listOpen();
    }

    @PostMapping("/complaints/{id}/accept")
    public DailyQuizReviewDto.Changed acceptComplaint(@PathVariable Long id, @RequestBody DailyQuizReviewDto.AcceptComplaintRequest request) {
        return new DailyQuizReviewDto.Changed(complaintService.accept(id, request.remember()));
    }

    @PostMapping("/complaints/{id}/reject")
    public void rejectComplaint(@PathVariable Long id, @RequestBody DailyQuizReviewDto.RejectComplaintRequest request) {
        complaintService.reject(id, request.note());
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

    @GetMapping("/sets/{id}/question-stats")
    public List<com.quizapp.dto.DailyQuizQuestionStatDto> questionStats(@PathVariable Long id) {
        return dailyQuizReviewService.listQuestionStatsForSet(id);
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
