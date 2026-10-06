package com.quizapp.repository;

import com.quizapp.model.DailyQuizResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailyQuizResultRepository extends JpaRepository<DailyQuizResult, Long> {

    Optional<DailyQuizResult> findByUserIdAndQuizDate(Long userId, LocalDate quizDate);

    // The weekly standings - everyone's results for a span of days, inclusive.
    List<DailyQuizResult> findByQuizDateBetween(LocalDate from, LocalDate to);

    // For account export/deletion.
    List<DailyQuizResult> findByUserIdOrderByQuizDateDesc(Long userId);

    @Modifying
    @Transactional
    void deleteByUserId(Long userId);

    // The retention job: old results are pruned too, just much later than the quizzes themselves.
    @Modifying
    @Transactional
    void deleteByQuizDateBefore(LocalDate cutoff);
}
