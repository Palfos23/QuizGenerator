package com.quizapp.dto;

import java.util.List;

public class DailyQuizScoreboardDto {

    private List<DailyQuizScoreboardEntryDto> entries;
    private double averageScore;
    private int maxScore;

    // Null if the requesting user hasn't been graded yet - only meaningful
    // once they have a GRADED attempt whose preference can be shown/changed.
    private Boolean yourLeaderboardPreference;

    // The expert's score for this day (shown apart from the ranking), or null if there's no expert or
    // they haven't been graded for it.
    private ExpertScore expert;

    public ExpertScore getExpert() {
        return expert;
    }

    public void setExpert(ExpertScore expert) {
        this.expert = expert;
    }

    public static class ExpertScore {
        private final String name;
        private final int score;
        private final int maxScore;

        public ExpertScore(String name, int score, int maxScore) {
            this.name = name;
            this.score = score;
            this.maxScore = maxScore;
        }

        public String getName() { return name; }
        public int getScore() { return score; }
        public int getMaxScore() { return maxScore; }
    }

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
