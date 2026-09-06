package com.quizapp.repository;

import com.quizapp.model.FlashbackYear;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface FlashbackYearRepository extends JpaRepository<FlashbackYear, Long> {

    // Lightweight projection for the admin list - counts hints via SIZE()
    // rather than loading each year's hint collection, same N+1-avoidance
    // reasoning as BullseyeQuestionRepository.findAllSummaries. Unfiltered
    // (includes excluded years, with a badge) - matches Bullseye's own admin
    // list, unlike findEligibleIds below which is for players.
    @Query("SELECT y.id as id, y.title as title, y.year as year, " +
           "SIZE(y.hints) as hintCount, y.excludedFromFlashback as excludedFromFlashback, " +
           "y.updatedAt as updatedAt FROM FlashbackYear y")
    List<FlashbackYearSummaryProjection> findAllSummaries();

    // ID-only, for random round-choice selection - mirrors
    // BullseyeQuestionRepository.findBattleEligibleIds. No category to filter
    // by - every Flashback year is a historic event, there's nothing else to
    // narrow down to.
    @Query("SELECT y.id FROM FlashbackYear y WHERE y.excludedFromFlashback = false")
    List<Long> findEligibleIds();
}
