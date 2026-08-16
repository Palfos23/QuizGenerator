package com.quizapp.repository;

import com.quizapp.model.Lineup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface LineupRepository extends JpaRepository<Lineup, Long> {

    // Every board that's live or already past, newest first - the solo
    // browsing list (and Previous/Next paging) draws from this, same
    // "visible" convention as Grid.findByWeekStartDateLessThanEqualOrderByWeekStartDateDesc.
    List<Lineup> findByWeekStartDateLessThanEqualOrderByWeekStartDateDescIdDesc(LocalDate date);

    // For the "remove this athlete before deleting" flow.
    @Query("SELECT DISTINCT l FROM Lineup l JOIN l.candidates c WHERE c.athlete.id = :athleteId")
    List<Lineup> findByCandidateAthleteId(Long athleteId);

    @Query("SELECT DISTINCT l FROM Lineup l JOIN l.entries e WHERE e.athlete.id = :athleteId")
    List<Lineup> findByEntryAthleteId(Long athleteId);
}
