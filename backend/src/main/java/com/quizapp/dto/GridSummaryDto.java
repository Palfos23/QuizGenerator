package com.quizapp.dto;

import com.quizapp.model.Sport;

import java.time.LocalDate;

public class GridSummaryDto {

    private Long id;
    private String title;
    private Sport sport;
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

    public GridSummaryDto(Long id, String title, Sport sport, LocalDate weekStartDate, int entryCount,
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

    public Sport getSport() {
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
}
