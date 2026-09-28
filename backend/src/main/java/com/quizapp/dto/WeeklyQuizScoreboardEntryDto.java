package com.quizapp.dto;

public class WeeklyQuizScoreboardEntryDto {

    private String userName;
    private int score;
    private int maxScore;
    private boolean isYou;

    public WeeklyQuizScoreboardEntryDto(String userName, int score, int maxScore, boolean isYou) {
        this.userName = userName;
        this.score = score;
        this.maxScore = maxScore;
        this.isYou = isYou;
    }

    public String getUserName() {
        return userName;
    }

    public int getScore() {
        return score;
    }

    public int getMaxScore() {
        return maxScore;
    }

    public boolean isYou() {
        return isYou;
    }
}
