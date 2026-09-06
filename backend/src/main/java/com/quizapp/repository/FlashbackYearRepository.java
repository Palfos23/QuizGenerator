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
    // list, unlike the eligible-only queries below which are for players.
    @Query("SELECT y.id as id, y.title as title, y.category as category, y.year as year, " +
           "SIZE(y.hints) as hintCount, y.excludedFromFlashback as excludedFromFlashback, " +
           "y.updatedAt as updatedAt FROM FlashbackYear y")
    List<FlashbackYearSummaryProjection> findAllSummaries();

    @Query("SELECT DISTINCT y.category FROM FlashbackYear y " +
           "WHERE y.category IS NOT NULL AND y.category <> '' ORDER BY y.category")
    List<String> findDistinctCategories();

    // ID-only queries for random round-choice selection - mirrors
    // TensionQuestionRepository's findIdsBy* methods exactly, so sampling
    // stays cheap regardless of how many years exist.
    @Query("SELECT y.id FROM FlashbackYear y WHERE y.excludedFromFlashback = false")
    List<Long> findEligibleIds();

    @Query("SELECT y.id FROM FlashbackYear y WHERE y.excludedFromFlashback = false AND lower(y.category) = lower(:category)")
    List<Long> findEligibleIdsByCategory(String category);

    // For "all categories except these" - used when a game excludes a few
    // categories rather than narrowing to just one.
    @Query("SELECT y.id FROM FlashbackYear y WHERE y.excludedFromFlashback = false " +
           "AND (y.category IS NULL OR lower(y.category) NOT IN :excludedLower)")
    List<Long> findEligibleIdsByCategoryNotIn(List<String> excludedLower);
}
