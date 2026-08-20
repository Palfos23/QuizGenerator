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

    // Lightweight projections for Grid Battle's pool listing and round-choice
    // picker - selecting just these columns (with entry count via a COUNT
    // subquery) never touches Grid.entries or GridEntry.athlete, both of which
    // are FetchType.EAGER on the entity itself. Loading full Grid entities via
    // findAll()/findAllById() would otherwise trigger a separate query per grid
    // (and per entry) to hydrate those eager associations - fine for one grid,
    // but N+1 across the whole table just to answer "how many are there" or
    // "give me 3 random ones".
    @Query("SELECT g.id as id, g.title as title, g.sport as sport, g.weekStartDate as weekStartDate, " +
           "(SELECT COUNT(e) FROM GridEntry e WHERE e.grid = g) as entryCount " +
           "FROM Grid g WHERE g.excludedFromGridBattle = false ORDER BY g.weekStartDate DESC")
    List<GridBattlePoolProjection> findBattleEligiblePool();

    @Query("SELECT g.id FROM Grid g WHERE g.excludedFromGridBattle = false")
    List<Long> findBattleEligibleIds();

    @Query("SELECT g.id as id, g.title as title, g.sport as sport, g.weekStartDate as weekStartDate, " +
           "(SELECT COUNT(e) FROM GridEntry e WHERE e.grid = g) as entryCount " +
           "FROM Grid g WHERE g.id IN :ids")
    List<GridBattlePoolProjection> findBattlePoolByIdIn(List<Long> ids);
}
