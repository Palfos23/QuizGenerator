package com.quizapp.repository;

import com.quizapp.model.DailyQuizAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
