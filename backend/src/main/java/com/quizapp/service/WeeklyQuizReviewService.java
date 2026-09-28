package com.quizapp.service;

import com.quizapp.dto.WeeklyQuizPendingAnswerDto;
import com.quizapp.exception.ResourceNotFoundException;
import com.quizapp.model.WeeklyQuizAnswer;
import com.quizapp.model.WeeklyQuizAnswerVerdict;
import com.quizapp.repository.WeeklyQuizAnswerRepository;
import com.quizapp.repository.WeeklyQuizAttemptRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

// Admin-facing grading queue for whichever Weekly Quiz answers weren't an
// exact match (see WeeklyQuizService.submitAnswers) - mirrors
// SubmittedQuestionService's PENDING-queue pattern.
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

    @Transactional(readOnly = true)
    public List<WeeklyQuizPendingAnswerDto> listPending() {
        return answerRepository.findByVerdict(WeeklyQuizAnswerVerdict.PENDING).stream()
                .map(a -> new WeeklyQuizPendingAnswerDto(
                        a.getId(),
                        a.getAttempt().getUser().getName(),
                        a.getQuestion().getQuestionText(),
                        a.getQuestion().getAnswer(),
                        a.getAnswerText()))
                .collect(Collectors.toList());
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
