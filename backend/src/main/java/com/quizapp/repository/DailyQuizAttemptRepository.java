package com.quizapp.repository;

import com.quizapp.model.DailyQuizAttempt;
import com.quizapp.model.DailyQuizAttemptStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailyQuizAttemptRepository extends JpaRepository<DailyQuizAttempt, Long> {
    Optional<DailyQuizAttempt> findBySet_IdAndUser_Email(Long setId, String email);

    // One query for every listed set's attempt by this user, rather than one query per set.
    List<DailyQuizAttempt> findBySet_IdInAndUser_Email(List<Long> setIds, String email);

    List<DailyQuizAttempt> findBySet_Id(Long setId);

    // For account export/deletion - every attempt this user has, across all days.
    List<DailyQuizAttempt> findByUser_Email(String email);

    // For the retention cleanup job.
    List<DailyQuizAttempt> findBySet_IdIn(List<Long> setIds);

    // A player's results that an admin finished reviewing and that they haven't looked at yet.
    List<DailyQuizAttempt> findByUser_EmailAndReviewResultUnseenTrue(String email);

    // Every attempt in a given state - used to back-fill the compact result records for attempts that
    // were graded before those existed.
    List<DailyQuizAttempt> findByStatus(DailyQuizAttemptStatus status);

    // Attempts in a state within a span of quiz days - the weekly standings use it to flag a week as
    // provisional while some of its answers are still waiting for an admin.
    List<DailyQuizAttempt> findByStatusAndSet_QuizDateBetween(DailyQuizAttemptStatus status, LocalDate from, LocalDate to);
}
