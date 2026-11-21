package com.quizapp.repository;

import com.quizapp.model.DailyQuizExclusion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

public interface DailyQuizExclusionRepository extends JpaRepository<DailyQuizExclusion, Long> {

    List<DailyQuizExclusion> findByQuizDate(LocalDate quizDate);

    @Modifying
    @Transactional
    void deleteByQuizDateBefore(LocalDate cutoff);
}
