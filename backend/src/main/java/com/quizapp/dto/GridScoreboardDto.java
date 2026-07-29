package com.quizapp.dto;

import java.util.List;

public class GridScoreboardDto {
    private List<GridScoreboardEntryDto> entries;
    private double averageScore;
    private int entryCount;

    // Null if the requesting user hasn't completed this grid at all yet -
    // only meaningful once they have an attempt whose preference can be shown/changed.
    private Boolean yourLeaderboardPreference;

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

    public Boolean getYourLeaderboardPreference() {
        return yourLeaderboardPreference;
    }

    public void setYourLeaderboardPreference(Boolean yourLeaderboardPreference) {
        this.yourLeaderboardPreference = yourLeaderboardPreference;
    }
}
