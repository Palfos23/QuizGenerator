package com.quizapp.repository;

import com.quizapp.model.Grid;
import com.quizapp.model.Sport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface GridRepository extends JpaRepository<Grid, Long> {
    List<Grid> findBySport(Sport sport);
    List<Grid> findByWeekStartDate(LocalDate weekStartDate);
}
