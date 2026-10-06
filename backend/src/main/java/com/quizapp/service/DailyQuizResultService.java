package com.quizapp.service;

import com.quizapp.model.DailyQuizAnswer;
import com.quizapp.model.DailyQuizAttempt;
import com.quizapp.model.DailyQuizAttemptStatus;
import com.quizapp.model.DailyQuizResult;
import com.quizapp.repository.DailyQuizAnswerRepository;
import com.quizapp.repository.DailyQuizAttemptRepository;
import com.quizapp.repository.DailyQuizResultRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Keeps the compact per-player-per-day result records (see DailyQuizResult) in step with the
 * attempts they summarise: written when an attempt is graded, refreshed whenever it's graded again
 * (an admin changing a decision) or its leaderboard choice changes. Those records are what the weekly
 * winner is calculated from, and they stay after the quiz itself has been deleted.
 */
@Service
public class DailyQuizResultService {

    private static final Logger log = LoggerFactory.getLogger(DailyQuizResultService.class);

    // The quizzes themselves are deleted after 7 days; these one-line records are kept much longer so
    // "last week's winner" and a few weeks of history remain - but not forever.
    static final int RESULT_RETENTION_DAYS = 60;

    private final DailyQuizResultRepository resultRepository;
    private final DailyQuizAttemptRepository attemptRepository;
    private final DailyQuizAnswerRepository answerRepository;

    public DailyQuizResultService(DailyQuizResultRepository resultRepository,
                                   DailyQuizAttemptRepository attemptRepository,
                                   DailyQuizAnswerRepository answerRepository) {
        this.resultRepository = resultRepository;
        this.attemptRepository = attemptRepository;
        this.answerRepository = answerRepository;
    }

    /** Creates or refreshes the record for a graded attempt. Safe to call any number of times. */
    @Transactional
    public void record(DailyQuizAttempt attempt) {
        if (attempt.getStatus() != DailyQuizAttemptStatus.GRADED) {
            return; // only final scores count - nothing to record until an admin has finished with it
        }
        Long userId = attempt.getUser().getId();
        LocalDate quizDate = attempt.getSet().getQuizDate();
        List<DailyQuizAnswer> answers = answerRepository.findByAttempt_IdOrderByIdAsc(attempt.getId());

        DailyQuizResult result = resultRepository.findByUserIdAndQuizDate(userId, quizDate).orElseGet(DailyQuizResult::new);
        result.setUserId(userId);
        result.setQuizDate(quizDate);
        result.setPlayerName(attempt.getUser().getName());
        result.setScore(attempt.getScore());
        result.setMaxScore(answers.stream().mapToInt(a -> DailyQuizScoring.maxPoints(a.getQuestion())).sum());
        result.setIncludeOnLeaderboard(attempt.isIncludeOnLeaderboard());
        resultRepository.save(result);
    }

    /** Mirrors the player's "show my name on the leaderboard" choice onto the record, if there is one yet. */
    @Transactional
    public void setIncludeOnLeaderboard(DailyQuizAttempt attempt, boolean include) {
        resultRepository.findByUserIdAndQuizDate(attempt.getUser().getId(), attempt.getSet().getQuizDate())
                .ifPresent(r -> {
                    r.setIncludeOnLeaderboard(include);
                    resultRepository.save(r);
                });
    }

    /**
     * Records every attempt that's already graded. Runs once at startup so attempts graded BEFORE
     * these records existed (the quizzes still in storage when this was introduced) count towards the
     * weekly standings too. Idempotent - later startups just refresh the same rows.
     */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void backfillFromGradedAttempts() {
        List<DailyQuizAttempt> graded = attemptRepository.findByStatus(DailyQuizAttemptStatus.GRADED);
        graded.forEach(this::record);
        if (!graded.isEmpty()) {
            log.info("Daily quiz results: synced {} graded attempt(s) into the weekly-standings records", graded.size());
        }
    }

    /** Deletes records older than RESULT_RETENTION_DAYS. */
    @Transactional
    public void pruneOld(LocalDate today) {
        resultRepository.deleteByQuizDateBefore(today.minusDays(RESULT_RETENTION_DAYS));
    }
}
