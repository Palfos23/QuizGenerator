package com.quizapp.repository;

import com.quizapp.model.DailyQuizSet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailyQuizSetRepository extends JpaRepository<DailyQuizSet, Long> {
    Optional<DailyQuizSet> findByQuizDate(LocalDate quizDate);

    // Repeat-avoidance when generating a new day's set - see DailyQuizService.
    List<DailyQuizSet> findByQuizDateAfter(LocalDate cutoff);

    // Past days for the archive list, most recent first - only those still within the retention
    // window (strictly after `oldest`, strictly before today).
    List<DailyQuizSet> findByQuizDateAfterAndQuizDateBeforeOrderByQuizDateDesc(LocalDate oldest, LocalDate today);

    // For the retention cleanup job - every quiz that has reached the maximum age.
    List<DailyQuizSet> findByQuizDateLessThanEqual(LocalDate cutoff);
}
