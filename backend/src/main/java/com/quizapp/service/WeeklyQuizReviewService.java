package com.quizapp.service;

import com.quizapp.dto.WeeklyQuizAttemptDetailDto;
import com.quizapp.dto.WeeklyQuizPendingAttemptDto;
import com.quizapp.exception.ResourceNotFoundException;
import com.quizapp.model.WeeklyQuizAnswer;
import com.quizapp.model.WeeklyQuizAnswerVerdict;
import com.quizapp.model.WeeklyQuizAttempt;
import com.quizapp.repository.WeeklyQuizAnswerRepository;
import com.quizapp.repository.WeeklyQuizAttemptRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// Admin-facing grading queue for whichever Weekly Quiz answers weren't an
// exact match (see WeeklyQuizService.submitAnswers) - mirrors
// SubmittedQuestionService's PENDING-queue pattern. Grouped by attempt
// (one player's one week) rather than a single flat list of answers, so it's
// always clear whose answers are being corrected - see listPendingAttempts.
@Service
public class WeeklyQuizReviewService {

    private final WeeklyQuizAnswerRepository answerRepository;
    private final WeeklyQuizAttemptRepository attemptRepository;
    private final WeeklyQuizService weeklyQuizService;

    public WeeklyQuizReviewService(WeeklyQuizAnswerRepository answerRepository,
                                    WeeklyQuizAttemptRepository attemptRepository,
                                    WeeklyQuizService weeklyQuizService) {
        this.answerRepository = answerRepository;
        this.attemptRepository = attemptRepository;
        this.weeklyQuizService = weeklyQuizService;
    }

    // One row per player+week that still has at least one PENDING answer -
    // disappears once every answer in that attempt has been resolved.
    @Transactional(readOnly = true)
    public List<WeeklyQuizPendingAttemptDto> listPendingAttempts() {
        Map<Long, List<WeeklyQuizAnswer>> byAttemptId = answerRepository.findByVerdict(WeeklyQuizAnswerVerdict.PENDING).stream()
                .collect(Collectors.groupingBy(a -> a.getAttempt().getId()));

        return byAttemptId.values().stream()
                .map(answers -> {
                    WeeklyQuizAttempt attempt = answers.get(0).getAttempt();
                    return new WeeklyQuizPendingAttemptDto(
                            attempt.getId(), attempt.getUser().getName(), attempt.getSet().getWeekStartDate(), answers.size());
                })
                .sorted(Comparator.comparing(WeeklyQuizPendingAttemptDto::getWeekStartDate).reversed()
                        .thenComparing(WeeklyQuizPendingAttemptDto::getPlayerName))
                .collect(Collectors.toList());
    }

    // Every answer for one player's one attempt, not just the pending ones -
    // gives the admin the full context (what they already got right/wrong)
    // alongside the handful still needing a decision.
    @Transactional(readOnly = true)
    public WeeklyQuizAttemptDetailDto getAttemptDetail(Long attemptId) {
        WeeklyQuizAttempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("No attempt found with id " + attemptId));
        List<WeeklyQuizAnswer> answers = answerRepository.findByAttempt_IdOrderByIdAsc(attemptId);

        List<WeeklyQuizAttemptDetailDto.AnswerDto> rows = new java.util.ArrayList<>();
        for (int i = 0; i < answers.size(); i++) {
            WeeklyQuizAnswer a = answers.get(i);
            rows.add(new WeeklyQuizAttemptDetailDto.AnswerDto(
                    a.getId(), i + 1, a.getQuestion().getQuestionText(), a.getAnswerText(),
                    a.getQuestion().getAnswer(), a.getVerdict().name()));
        }
        return new WeeklyQuizAttemptDetailDto(attempt.getId(), attempt.getUser().getName(), attempt.getSet().getWeekStartDate(), rows);
    }

    @Transactional
    public void resolve(Long answerId, boolean correct) {
        WeeklyQuizAnswer answer = answerRepository.findById(answerId)
                .orElseThrow(() -> new ResourceNotFoundException("No pending answer found with id " + answerId));
        if (answer.getVerdict() != WeeklyQuizAnswerVerdict.PENDING) {
            throw new IllegalStateException("This answer has already been resolved.");
        }
        answer.setVerdict(correct ? WeeklyQuizAnswerVerdict.CORRECT : WeeklyQuizAnswerVerdict.INCORRECT);
        answerRepository.save(answer);

        long stillPending = answerRepository.countByAttempt_IdAndVerdict(
                answer.getAttempt().getId(), WeeklyQuizAnswerVerdict.PENDING);
        if (stillPending == 0) {
            var attempt = answer.getAttempt();
            weeklyQuizService.gradeAttempt(attempt);
            attemptRepository.save(attempt);
        }
    }
}
