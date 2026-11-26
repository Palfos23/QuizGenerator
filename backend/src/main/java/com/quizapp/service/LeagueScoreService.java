package com.quizapp.service;

import com.quizapp.model.DailyQuizAttempt;
import com.quizapp.model.DailyQuizAttemptStatus;
import com.quizapp.model.LeagueDayScore;
import com.quizapp.repository.DailyQuizAnswerRepository;
import com.quizapp.repository.DailyQuizAttemptRepository;
import com.quizapp.repository.LeagueDayScoreRepository;
import com.quizapp.repository.LeagueMemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Keeps the league day scores (LeagueDayScore) in step with graded daily-quiz attempts. Called from
 * DailyQuizResultService whenever an attempt is graded or regraded, and for a new member's recent attempts
 * when they join a league.
 *
 * The league deadline lives here: a quiz counts only if it was handed in on its own day - answering
 * tomorrow's quiz late, after seeing someone's answers, scores 0 in every league (it still counts on the
 * global leaderboard as before).
 */
@Service
public class LeagueScoreService {

    private final LeagueDayScoreRepository dayScoreRepository;
    private final LeagueMemberRepository memberRepository;
    private final DailyQuizAttemptRepository attemptRepository;
    private final DailyQuizAnswerRepository answerRepository;
    private final ExpertConfig expertConfig;

    public LeagueScoreService(LeagueDayScoreRepository dayScoreRepository,
                              LeagueMemberRepository memberRepository,
                              DailyQuizAttemptRepository attemptRepository,
                              DailyQuizAnswerRepository answerRepository,
                              ExpertConfig expertConfig) {
        this.dayScoreRepository = dayScoreRepository;
        this.memberRepository = memberRepository;
        this.attemptRepository = attemptRepository;
        this.answerRepository = answerRepository;
        this.expertConfig = expertConfig;
    }

    /** True when the attempt was handed in on the quiz's own day (the same clock that decides which quiz is "today"). */
    public static boolean submittedOnItsDay(DailyQuizAttempt attempt) {
        return attempt.getSubmittedAt() != null
                && LocalDate.ofInstant(attempt.getSubmittedAt(), ZoneId.systemDefault()).equals(attempt.getSet().getQuizDate());
    }

    /** Records (or refreshes) a graded attempt's score for leagues, if it counts. Safe to call any number of times. */
    @Transactional
    public void record(DailyQuizAttempt attempt) {
        if (attempt.getStatus() != DailyQuizAttemptStatus.GRADED || !submittedOnItsDay(attempt)) {
            return;
        }
        Long userId = attempt.getUser().getId();
        // Only league players are kept (for good), plus the Expert, who is the opponent for the odd one out
        if (!expertConfig.isExpert(userId) && !memberRepository.existsByUserId(userId)) {
            return;
        }
        LocalDate quizDate = attempt.getSet().getQuizDate();
        LeagueDayScore row = dayScoreRepository.findByUserIdAndQuizDate(userId, quizDate).orElseGet(LeagueDayScore::new);
        row.setUserId(userId);
        row.setQuizDate(quizDate);
        row.setScore(attempt.getScore());
        row.setMaxScore(answerRepository.findByAttempt_IdOrderByIdAsc(attempt.getId()).stream()
                .mapToInt(a -> DailyQuizScoring.maxPoints(a.getQuestion())).sum());
        dayScoreRepository.save(row);
    }

    /** A player just joined a league: bring in their recent graded quizzes (the ones still stored, the last 7 days). */
    @Transactional
    public void syncUser(String email) {
        attemptRepository.findByUser_Email(email).forEach(this::record);
    }
}
