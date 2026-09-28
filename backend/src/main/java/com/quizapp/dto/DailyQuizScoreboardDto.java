package com.quizapp.dto;

import java.util.List;

public class DailyQuizScoreboardDto {

    private List<DailyQuizScoreboardEntryDto> entries;
    private double averageScore;
    private int maxScore;

    // Null if the requesting user hasn't been graded yet - only meaningful
    // once they have a GRADED attempt whose preference can be shown/changed.
    private Boolean yourLeaderboardPreference;

    public DailyQuizScoreboardDto(List<DailyQuizScoreboardEntryDto> entries, double averageScore, int maxScore) {
        this.entries = entries;
        this.averageScore = averageScore;
        this.maxScore = maxScore;
    }

    public List<DailyQuizScoreboardEntryDto> getEntries() {
        return entries;
    }

    public double getAverageScore() {
        return averageScore;
    }

    public int getMaxScore() {
        return maxScore;
    }

    public Boolean getYourLeaderboardPreference() {
        return yourLeaderboardPreference;
    }

    public void setYourLeaderboardPreference(Boolean yourLeaderboardPreference) {
        this.yourLeaderboardPreference = yourLeaderboardPreference;
    }
}
