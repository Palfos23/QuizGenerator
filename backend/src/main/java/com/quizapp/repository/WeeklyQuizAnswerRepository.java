package com.quizapp.repository;

import com.quizapp.model.WeeklyQuizAnswer;
import com.quizapp.model.WeeklyQuizAnswerVerdict;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface WeeklyQuizAnswerRepository extends JpaRepository<WeeklyQuizAnswer, Long> {
    // Ordered by id (= insertion order = the same order submitAnswers() looped
    // over set.getQuestionIds() in) so the numbered results list matches the
    // numbered questions the player actually answered - plain findByAttempt_Id
    // has no guaranteed row order, which is what let a graded quiz's reveal
    // scramble the question order relative to how it was presented.
    List<WeeklyQuizAnswer> findByAttempt_IdOrderByIdAsc(Long attemptId);

    List<WeeklyQuizAnswer> findByVerdict(WeeklyQuizAnswerVerdict verdict);

    long countByAttempt_IdAndVerdict(Long attemptId, WeeklyQuizAnswerVerdict verdict);

    // For account deletion - child rows must go before their parent attempt.
    @Modifying
    @Transactional
    void deleteByAttempt_Id(Long attemptId);
}
