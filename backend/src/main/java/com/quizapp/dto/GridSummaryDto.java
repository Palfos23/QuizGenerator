package com.quizapp.dto;


import java.time.Instant;
import java.time.LocalDate;

public class GridSummaryDto {

    private Long id;
    private String title;
    private String sport;
    private LocalDate weekStartDate;
    private int entryCount;

    // Only meaningful on the user-facing active/archive lists - null on the admin
    // management list, where "your own progress" isn't a relevant concept.
    private String status; // "NOT_STARTED" | "IN_PROGRESS" | "COMPLETED"
    private Integer guessedCount;

    // Only set on the admin management list - lets an admin see strike count
    // at a glance without opening each grid to edit it.
    private Integer maxStrikes;

    public Integer getMaxStrikes() {
        return maxStrikes;
    }

    public void setMaxStrikes(Integer maxStrikes) {
        this.maxStrikes = maxStrikes;
    }

    // Whether this grid should be hidden from Grid Battle's random/manual pick
    // pool - set once a newer, corrected version of this grid exists. Doesn't
    // affect visibility on the regular Weekly Grid page at all.
    private boolean excludedFromGridBattle;

    public boolean isExcludedFromGridBattle() {
        return excludedFromGridBattle;
    }

    public void setExcludedFromGridBattle(boolean excludedFromGridBattle) {
        this.excludedFromGridBattle = excludedFromGridBattle;
    }

    // Only set on the admin management list - lets an admin see at a glance
    // which grids auto-draw their whole candidate pool from a category
    // instead of an explicitly curated list.
    private boolean entireCategoryPool;

    public boolean isEntireCategoryPool() {
        return entireCategoryPool;
    }

    public void setEntireCategoryPool(boolean entireCategoryPool) {
        this.entireCategoryPool = entireCategoryPool;
    }

    public GridSummaryDto(Long id, String title, String sport, LocalDate weekStartDate, int entryCount,
                           String status, Integer guessedCount) {
        this.id = id;
        this.title = title;
        this.sport = sport;
        this.weekStartDate = weekStartDate;
        this.entryCount = entryCount;
        this.status = status;
        this.guessedCount = guessedCount;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getSport() {
        return sport;
    }

    public LocalDate getWeekStartDate() {
        return weekStartDate;
    }

    public int getEntryCount() {
        return entryCount;
    }

    public String getStatus() {
        return status;
    }

    public Integer getGuessedCount() {
        return guessedCount;
    }

    // When this grid's content was last saved - shown to players as a
    // "might be outdated" signal, and to admins on the management list.
    private Instant updatedAt;

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
