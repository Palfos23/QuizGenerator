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

    // Lightweight projection version of the above, plus the battle-eligible
    // pool/round-choice picker - Lineup.entries and LineupEntry.athlete are
    // both FetchType.EAGER, so hydrating full Lineup entities via the entity
    // query above (or findAll()) issues a separate query per board (and per
    // entry) just to list titles/scores or count entries. Mirrors
    // GridRepository's equivalent projection queries.
    @Query("SELECT l.id as id, l.title as title, l.competition as competition, l.teamName as teamName, " +
           "l.opponentName as opponentName, l.scoreFor as scoreFor, l.scoreAgainst as scoreAgainst, " +
           "l.matchDate as matchDate, l.weekStartDate as weekStartDate, l.formation as formation, " +
           "(SELECT COUNT(e) FROM LineupEntry e WHERE e.lineup = l) as entryCount, " +
           "l.excludedFromBattle as excludedFromBattle " +
           "FROM Lineup l WHERE l.weekStartDate <= :date ORDER BY l.weekStartDate DESC, l.id DESC")
    List<LineupSummaryProjection> findSummariesByWeekStartDateLessThanEqual(LocalDate date);

    @Query("SELECT l.id FROM Lineup l WHERE l.excludedFromBattle = false")
    List<Long> findBattleEligibleIds();

    @Query("SELECT l.id as id, l.title as title, l.competition as competition, l.teamName as teamName, " +
           "l.opponentName as opponentName, l.scoreFor as scoreFor, l.scoreAgainst as scoreAgainst, " +
           "l.matchDate as matchDate, l.weekStartDate as weekStartDate, l.formation as formation, " +
           "(SELECT COUNT(e) FROM LineupEntry e WHERE e.lineup = l) as entryCount, " +
           "l.excludedFromBattle as excludedFromBattle " +
           "FROM Lineup l WHERE l.id IN :ids")
    List<LineupSummaryProjection> findSummariesByIdIn(List<Long> ids);

    // For the "remove this athlete before deleting" flow.
    @Query("SELECT DISTINCT l FROM Lineup l JOIN l.candidates c WHERE c.athlete.id = :athleteId")
    List<Lineup> findByCandidateAthleteId(Long athleteId);

    @Query("SELECT DISTINCT l FROM Lineup l JOIN l.entries e WHERE e.athlete.id = :athleteId")
    List<Lineup> findByEntryAthleteId(Long athleteId);

    // For propagating a newly-added pool member out to every board that
    // previously imported from that pool - mirrors GridRepository.findByLinkedPoolId.
    @Query("SELECT DISTINCT l FROM Lineup l JOIN l.linkedPools p WHERE p.id = :poolId")
    List<Lineup> findByLinkedPoolId(Long poolId);
}
