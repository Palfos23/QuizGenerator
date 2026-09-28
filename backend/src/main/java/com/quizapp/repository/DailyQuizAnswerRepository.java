package com.quizapp.repository;

import com.quizapp.model.DailyQuizAnswer;
import com.quizapp.model.DailyQuizAnswerVerdict;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface DailyQuizAnswerRepository extends JpaRepository<DailyQuizAnswer, Long> {
    // Ordered by id (= insertion order = the same order submitAnswers() looped
    // over set.getQuestionIds() in) so the numbered results list matches the
    // numbered questions the player actually answered - plain findByAttempt_Id
    // has no guaranteed row order, which is what let a graded quiz's reveal
    // scramble the question order relative to how it was presented.
    List<DailyQuizAnswer> findByAttempt_IdOrderByIdAsc(Long attemptId);

    List<DailyQuizAnswer> findByVerdict(DailyQuizAnswerVerdict verdict);

    long countByAttempt_IdAndVerdict(Long attemptId, DailyQuizAnswerVerdict verdict);

    // For account deletion - child rows must go before their parent attempt.
    @Modifying
    @Transactional
    void deleteByAttempt_Id(Long attemptId);

    // For the retention cleanup job - bulk-deletes every answer belonging to
    // any of a batch of stale attempts in one statement.
    @Modifying
    @Transactional
    void deleteByAttempt_IdIn(List<Long> attemptIds);
}
