package com.quizapp.repository;

import com.quizapp.dto.DailyQuizReviewRow;
import com.quizapp.model.DailyQuizAnswer;
import com.quizapp.model.DailyQuizAnswerVerdict;
import com.quizapp.model.DailyQuizAttemptStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

public interface DailyQuizAnswerRepository extends JpaRepository<DailyQuizAnswer, Long> {
    // Ordered by id (= insertion order = the same order submitAnswers() looped
    // over set.getQuestionIds() in) so the numbered results list matches the
    // numbered questions the player actually answered - plain findByAttempt_Id
    // has no guaranteed row order, which is what let a graded quiz's reveal
    // scramble the question order relative to how it was presented.
    List<DailyQuizAnswer> findByAttempt_IdOrderByIdAsc(Long attemptId);

    List<DailyQuizAnswer> findByVerdict(DailyQuizAnswerVerdict verdict);

    // Every answer ever given to one question (admin review across days). Its attempt and quiz come with it:
    // deciding an answer looks at both, and one query per answer is what made a popular question slow to settle.
    @Query("SELECT a FROM DailyQuizAnswer a JOIN FETCH a.attempt t JOIN FETCH t.set WHERE a.question.id = :questionId")
    List<DailyQuizAnswer> findByQuestion_Id(@Param("questionId") Long questionId);

    // Everything the answer-review page lists, as plain rows in one query: every answer on the given quizzes from
    // players who have handed in (not those still playing), oldest first.
    @Query("SELECT new com.quizapp.dto.DailyQuizReviewRow(a.attempt.set.id, a.question.id, a.answerText, a.verdict) "
            + "FROM DailyQuizAnswer a WHERE a.attempt.set.id IN :setIds AND a.attempt.status <> :excluded ORDER BY a.id")
    List<DailyQuizReviewRow> findReviewRows(@Param("setIds") Collection<Long> setIds, @Param("excluded") DailyQuizAttemptStatus excluded);

    long countByAttempt_IdAndVerdict(Long attemptId, DailyQuizAnswerVerdict verdict);

    // An attempt's score: the points of every answer judged CORRECT (a Year question can be
    // worth 2, everything else 1).
    @Query("SELECT COALESCE(SUM(a.points), 0) FROM DailyQuizAnswer a WHERE a.attempt.id = :attemptId AND a.verdict = :verdict")
    long sumPointsByAttemptAndVerdict(@Param("attemptId") Long attemptId, @Param("verdict") DailyQuizAnswerVerdict verdict);

    // One query for a whole batch of attempts (the scoreboard) instead of one per row.
    List<DailyQuizAnswer> findByAttempt_IdIn(List<Long> attemptIds);

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
