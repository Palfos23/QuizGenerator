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
