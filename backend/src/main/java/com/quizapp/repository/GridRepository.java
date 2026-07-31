package com.quizapp.repository;

import com.quizapp.model.Grid;
import com.quizapp.model.Sport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface GridRepository extends JpaRepository<Grid, Long> {
    List<Grid> findBySport(Sport sport);
    List<Grid> findByWeekStartDate(LocalDate weekStartDate);

    // For the "remove this athlete from grids before deleting" flow - every
    // grid that references this athlete at all (as a candidate, an entry, or
    // both), so the admin can be shown exactly what's affected before choosing.
    @Query("SELECT DISTINCT g FROM Grid g LEFT JOIN g.candidates c LEFT JOIN g.entries e " +
           "WHERE c.athlete.id = :athleteId OR e.athlete.id = :athleteId")
    List<Grid> findDistinctByAthleteId(Long athleteId);
}
