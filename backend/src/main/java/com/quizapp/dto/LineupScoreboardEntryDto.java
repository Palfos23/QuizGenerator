package com.quizapp.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class LineupScoreboardEntryDto {

    private String userName;
    private int guessedCount;
    private int entryCount;
    private boolean completed;
    private boolean isYou;

    public LineupScoreboardEntryDto(String userName, int guessedCount, int entryCount, boolean completed, boolean isYou) {
        this.userName = userName;
        this.guessedCount = guessedCount;
        this.entryCount = entryCount;
        this.completed = completed;
        this.isYou = isYou;
    }

    public String getUserName() {
        return userName;
    }

    public int getGuessedCount() {
        return guessedCount;
    }

    public int getEntryCount() {
        return entryCount;
    }

    public boolean isCompleted() {
        return completed;
    }

    // Jackson would otherwise name this property "you" (it drops the "is" from a boolean getter),
    // but the frontend's leaderboards read `isYou` - with the mismatch, a player's own row was never
    // highlighted and the "your rank" row below the top 5 never appeared.
    @JsonProperty("isYou")
    public boolean isYou() {
        return isYou;
    }
}
