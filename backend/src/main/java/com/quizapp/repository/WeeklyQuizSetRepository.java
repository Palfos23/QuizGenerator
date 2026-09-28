package com.quizapp.repository;

import com.quizapp.model.WeeklyQuizSet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface WeeklyQuizSetRepository extends JpaRepository<WeeklyQuizSet, Long> {
    Optional<WeeklyQuizSet> findByWeekStartDate(LocalDate weekStartDate);

    // Repeat-avoidance when generating a new week's set - see WeeklyQuizService.
    List<WeeklyQuizSet> findByWeekStartDateAfter(LocalDate cutoff);
}
