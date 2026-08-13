package com.quizapp.repository;

import com.quizapp.model.Grid;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

public interface GridRepository extends JpaRepository<Grid, Long> {
    List<Grid> findBySport(String sport);
    List<Grid> findByWeekStartDate(LocalDate weekStartDate);
    boolean existsBySport(String sport);

    // Every grid that's live or already past, newest first. Used both by the
    // player-facing weekly grid list and by the attempt-cleanup job, so the two
    // always agree on exactly which grids are "visible" without risk of drifting
    // apart from each other over time.
    List<Grid> findByWeekStartDateLessThanEqualOrderByWeekStartDateDesc(LocalDate date);

    @Modifying
    @Transactional
    @Query("UPDATE Grid g SET g.sport = :newName WHERE g.sport = :oldName")
    int renameSport(String oldName, String newName);

    // For the "remove this athlete from grids before deleting" flow - every
    // grid that references this athlete at all, as a candidate or an entry.
    // Two separate simple queries rather than one query joining both
    // collections at once - joining two @OneToMany collections of the same
    // entity simultaneously produces every combination of the two collections
    // before any deduplication happens, which is both wasteful and risky for
    // a grid with a large candidate pool.
    @Query("SELECT DISTINCT g FROM Grid g JOIN g.candidates c WHERE c.athlete.id = :athleteId")
    List<Grid> findByCandidateAthleteId(Long athleteId);

    @Query("SELECT DISTINCT g FROM Grid g JOIN g.entries e WHERE e.athlete.id = :athleteId")
    List<Grid> findByEntryAthleteId(Long athleteId);

    // For propagating a newly-added pool member out to every grid that
    // previously imported from that pool.
    @Query("SELECT DISTINCT g FROM Grid g JOIN g.linkedPools p WHERE p.id = :poolId")
    List<Grid> findByLinkedPoolId(Long poolId);
}
