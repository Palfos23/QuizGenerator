package com.quizapp.repository;

import com.quizapp.model.WeeklyQuizAnswer;
import com.quizapp.model.WeeklyQuizAnswerVerdict;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface WeeklyQuizAnswerRepository extends JpaRepository<WeeklyQuizAnswer, Long> {
    List<WeeklyQuizAnswer> findByAttempt_Id(Long attemptId);

    List<WeeklyQuizAnswer> findByVerdict(WeeklyQuizAnswerVerdict verdict);

    long countByAttempt_IdAndVerdict(Long attemptId, WeeklyQuizAnswerVerdict verdict);

    // For account deletion - child rows must go before their parent attempt.
    @Modifying
    @Transactional
    void deleteByAttempt_Id(Long attemptId);
}
