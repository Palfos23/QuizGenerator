package com.quizapp.repository;

import com.quizapp.model.WeeklyQuizAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WeeklyQuizAttemptRepository extends JpaRepository<WeeklyQuizAttempt, Long> {
    Optional<WeeklyQuizAttempt> findBySet_IdAndUser_Email(Long setId, String email);

    // For account export/deletion - every attempt this user has, across all weeks.
    List<WeeklyQuizAttempt> findByUser_Email(String email);
}
