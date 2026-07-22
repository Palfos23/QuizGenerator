package com.quizapp.dto;

import com.quizapp.model.Sport;

import java.time.LocalDate;

public class GridSummaryDto {

    private Long id;
    private String title;
    private Sport sport;
    private LocalDate weekStartDate;
    private int entryCount;

    public GridSummaryDto(Long id, String title, Sport sport, LocalDate weekStartDate, int entryCount) {
        this.id = id;
        this.title = title;
        this.sport = sport;
        this.weekStartDate = weekStartDate;
        this.entryCount = entryCount;
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
}
