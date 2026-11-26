package com.quizapp.controller;

import com.quizapp.dto.DailyQuizAttemptDetailDto;
import com.quizapp.dto.DailyQuizDayAttemptsDto;
import com.quizapp.dto.DailyQuizPendingAttemptDto;
import com.quizapp.dto.DailyQuizResolveRequest;
import com.quizapp.dto.DailyQuizReviewDto;
import com.quizapp.dto.DailyQuizPreviewDto;
import com.quizapp.service.DailyQuizComplaintService;
import com.quizapp.service.DailyQuizPreviewService;
import com.quizapp.service.DailyQuizReviewService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/daily-quiz")
public class AdminDailyQuizController {

    private final DailyQuizReviewService dailyQuizReviewService;
    private final DailyQuizComplaintService complaintService;
    private final DailyQuizPreviewService previewService;

    public AdminDailyQuizController(DailyQuizReviewService dailyQuizReviewService, DailyQuizComplaintService complaintService,
                                    DailyQuizPreviewService previewService) {
        this.dailyQuizReviewService = dailyQuizReviewService;
        this.complaintService = complaintService;
        this.previewService = previewService;
    }

    // ---- tomorrow's quiz, before it goes live: look it over, swap questions, rule out a type for the day ----

    @GetMapping("/preview")
    public DailyQuizPreviewDto preview() {
        return previewService.get();
    }

    @PostMapping("/preview/replace")
    public DailyQuizPreviewDto replace(@RequestBody DailyQuizPreviewDto.ReplaceRequest request) {
        return previewService.replace(request.questionId());
    }

    @PostMapping("/preview/choose")
    public DailyQuizPreviewDto choose(@RequestBody DailyQuizPreviewDto.ChooseRequest request) {
        return previewService.choose(request.questionId(), request.newQuestionId());
    }

    @GetMapping("/preview/candidates")
    public List<DailyQuizPreviewDto.Candidate> candidates(@RequestParam Long forQuestionId, @RequestParam(defaultValue = "") String term) {
        return previewService.candidates(forQuestionId, term);
    }

    @PostMapping("/preview/exclusions")
    public DailyQuizPreviewDto addExclusion(@RequestBody DailyQuizPreviewDto.ExclusionRequest request) {
        return previewService.addExclusion(request.kind(), request.value());
    }

    @PostMapping("/preview/exclusions/remove")
    public DailyQuizPreviewDto removeExclusion(@RequestBody DailyQuizPreviewDto.ExclusionRequest request) {
        return previewService.removeExclusion(request.kind(), request.value());
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

    @PostMapping("/answer-groups/decide-many")
    public DailyQuizReviewDto.Changed decideMany(@RequestBody DailyQuizReviewDto.DecideManyRequest request) {
        return new DailyQuizReviewDto.Changed(dailyQuizReviewService.decideGroups(request.decisions()));
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
    public DailyQuizDayAttemptsDto attemptsForSet(@PathVariable Long id,
                                                  @RequestParam(defaultValue = "") String q,
                                                  @RequestParam(defaultValue = "ALL") String filter,
                                                  @RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "50") int size) {
        return dailyQuizReviewService.listAttemptsForSet(id, q, filter, page, size);
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

    public record YearPointsRequest(int points) {
    }

    // Override the automatic 2 / 1 / 0 on a Year answer (e.g. "88" written for 1988).
    @PostMapping("/answers/{id}/year-points")
    public void setYearPoints(@PathVariable Long id, @RequestBody YearPointsRequest request) {
        dailyQuizReviewService.setYearPoints(id, request.points());
    }
}
