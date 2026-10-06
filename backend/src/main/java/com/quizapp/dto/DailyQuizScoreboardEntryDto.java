package com.quizapp.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DailyQuizScoreboardEntryDto {

    private String userName;
    private int score;
    private int maxScore;
    private boolean isYou;

    public DailyQuizScoreboardEntryDto(String userName, int score, int maxScore, boolean isYou) {
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

    // Jackson would otherwise name this property "you" (it drops the "is" from a boolean getter),
    // but the frontend's leaderboards read `isYou` - with the mismatch, a player's own row was never
    // highlighted and the "your rank" row below the top 5 never appeared.
    @JsonProperty("isYou")
    public boolean isYou() {
        return isYou;
    }
}
