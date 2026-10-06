package com.quizapp.service;

import com.quizapp.dto.DailyQuizAttemptDetailDto;
import com.quizapp.dto.DailyQuizPendingAttemptDto;
import com.quizapp.exception.ResourceNotFoundException;
import com.quizapp.model.DailyQuizAnswer;
import com.quizapp.model.DailyQuizAnswerVerdict;
import com.quizapp.model.DailyQuizAttempt;
import com.quizapp.repository.DailyQuizAnswerRepository;
import com.quizapp.repository.DailyQuizAttemptRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// Admin-facing grading queue for whichever Daily Quiz answers weren't an
// exact match (see DailyQuizService.submitAnswers) - mirrors
// SubmittedQuestionService's PENDING-queue pattern. Grouped by attempt (one
// player's one day) rather than a single flat list of answers, so it's
// always clear whose answers are being corrected - see listPendingAttempts.
@Service
public class DailyQuizReviewService {

    private final DailyQuizAnswerRepository answerRepository;
    private final DailyQuizAttemptRepository attemptRepository;
    private final DailyQuizService dailyQuizService;

    public DailyQuizReviewService(DailyQuizAnswerRepository answerRepository,
                                   DailyQuizAttemptRepository attemptRepository,
                                   DailyQuizService dailyQuizService) {
        this.answerRepository = answerRepository;
        this.attemptRepository = attemptRepository;
        this.dailyQuizService = dailyQuizService;
    }

    // One row per player+day that still has at least one PENDING answer -
    // disappears once every answer in that attempt has been resolved.
    @Transactional(readOnly = true)
    public List<DailyQuizPendingAttemptDto> listPendingAttempts() {
        Map<Long, List<DailyQuizAnswer>> byAttemptId = answerRepository.findByVerdict(DailyQuizAnswerVerdict.PENDING).stream()
                .collect(Collectors.groupingBy(a -> a.getAttempt().getId()));

        return byAttemptId.values().stream()
                .map(answers -> {
                    DailyQuizAttempt attempt = answers.get(0).getAttempt();
                    return new DailyQuizPendingAttemptDto(
                            attempt.getId(), attempt.getUser().getName(), attempt.getSet().getQuizDate(), answers.size());
                })
                .sorted(Comparator.comparing(DailyQuizPendingAttemptDto::getQuizDate).reversed()
                        .thenComparing(DailyQuizPendingAttemptDto::getPlayerName))
                .collect(Collectors.toList());
    }

    // Every answer for one player's one attempt, not just the pending ones -
    // gives the admin the full context (what they already got right/wrong)
    // alongside the handful still needing a decision.
    @Transactional(readOnly = true)
    public DailyQuizAttemptDetailDto getAttemptDetail(Long attemptId) {
        DailyQuizAttempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("No attempt found with id " + attemptId));
        List<DailyQuizAnswer> answers = answerRepository.findByAttempt_IdOrderByIdAsc(attemptId);

        List<DailyQuizAttemptDetailDto.AnswerDto> rows = new java.util.ArrayList<>();
        for (int i = 0; i < answers.size(); i++) {
            DailyQuizAnswer a = answers.get(i);
            rows.add(new DailyQuizAttemptDetailDto.AnswerDto(
                    a.getId(), i + 1, a.getQuestion().getQuestionText(), a.getAnswerText(),
                    a.getQuestion().getAnswer(), a.getVerdict().name(), a.getQuestion().getPhotoUrl(),
                    DailyQuizScoring.isScoredAsYear(a.getQuestion()), a.getPoints()));
        }
        return new DailyQuizAttemptDetailDto(attempt.getId(), attempt.getSet().getId(), attempt.getUser().getName(), attempt.getSet().getQuizDate(), rows);
    }

    @Transactional
    public void resolve(Long answerId, boolean correct) {
        DailyQuizAnswer answer = answerRepository.findById(answerId)
                .orElseThrow(() -> new ResourceNotFoundException("No pending answer found with id " + answerId));
        if (answer.getVerdict() != DailyQuizAnswerVerdict.PENDING) {
            throw new IllegalStateException("This answer has already been resolved.");
        }
        answer.setVerdict(correct ? DailyQuizAnswerVerdict.CORRECT : DailyQuizAnswerVerdict.INCORRECT);
        answerRepository.save(answer);

        long stillPending = answerRepository.countByAttempt_IdAndVerdict(
                answer.getAttempt().getId(), DailyQuizAnswerVerdict.PENDING);
        if (stillPending == 0) {
            var attempt = answer.getAttempt();
            dailyQuizService.gradeAttempt(attempt);
            attemptRepository.save(attempt);
        }
    }
}
