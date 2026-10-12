package com.quizapp.repository;

import com.quizapp.model.DailyQuizQuestionUse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

public interface DailyQuizQuestionUseRepository extends JpaRepository<DailyQuizQuestionUse, Long> {

    // Everything used on a day after `after` up to and including `through` (the day being generated).
    List<DailyQuizQuestionUse> findByQuizDateAfterAndQuizDateLessThanEqual(LocalDate after, LocalDate through);

    List<DailyQuizQuestionUse> findByQuizDate(LocalDate quizDate);

    @Modifying
    @Transactional
    void deleteByQuizDateBefore(LocalDate cutoff);
}
