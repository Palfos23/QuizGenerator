package com.quizapp.dto;

import java.util.List;

public class GridScoreboardDto {
    private List<GridScoreboardEntryDto> entries;
    private double averageScore;
    private int entryCount;

    public GridScoreboardDto(List<GridScoreboardEntryDto> entries, double averageScore, int entryCount) {
        this.entries = entries;
        this.averageScore = averageScore;
        this.entryCount = entryCount;
    }

    public List<GridScoreboardEntryDto> getEntries() {
        return entries;
    }

    public double getAverageScore() {
        return averageScore;
    }

    public int getEntryCount() {
        return entryCount;
    }
}
