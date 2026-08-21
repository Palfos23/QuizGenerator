package com.quizapp.repository;

import com.quizapp.model.BullseyeQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface BullseyeQuestionRepository extends JpaRepository<BullseyeQuestion, Long> {

    boolean existsBySport(String sport);

    @Modifying
    @Transactional
    @Query("UPDATE BullseyeQuestion q SET q.sport = :newName WHERE q.sport = :oldName")
    int renameSport(String oldName, String newName);

    // Lightweight projection for every question-listing screen - selecting just
    // these columns (with entry count via a COUNT subquery) never touches
    // BullseyeQuestion.entries or BullseyeEntry.athlete, both of which are
    // FetchType.EAGER on the entity itself. Loading full entities via findAll()
    // would otherwise trigger a separate query per question (and per entry) to
    // hydrate those eager associations - same N+1 GridRepository was fixed for
    // this session (see GridSummaryProjection).
    @Query("SELECT q.id as id, q.title as title, q.sport as sport, q.targetValue as targetValue, " +
           "q.statLabel as statLabel, (SELECT COUNT(e) FROM BullseyeEntry e WHERE e.question = q) as entryCount, " +
           "q.excludedFromBullseye as excludedFromBullseye, q.entireCategoryPool as entireCategoryPool " +
           "FROM BullseyeQuestion q")
    List<BullseyeQuestionSummaryProjection> findAllSummaries();

    @Query("SELECT q.id FROM BullseyeQuestion q WHERE q.excludedFromBullseye = false")
    List<Long> findBattleEligibleIds();

    @Query("SELECT q.id as id, q.title as title, q.sport as sport, q.targetValue as targetValue, " +
           "q.statLabel as statLabel, (SELECT COUNT(e) FROM BullseyeEntry e WHERE e.question = q) as entryCount, " +
           "q.excludedFromBullseye as excludedFromBullseye, q.entireCategoryPool as entireCategoryPool " +
           "FROM BullseyeQuestion q WHERE q.id IN :ids")
    List<BullseyeQuestionSummaryProjection> findSummariesByIdIn(List<Long> ids);
}
