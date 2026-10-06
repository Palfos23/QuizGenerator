package com.quizapp.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class GridScoreboardEntryDto {

    private String userName;
    private int guessedCount;
    private int entryCount;
    private boolean completed;
    private boolean usedOvertime;
    private int overtimeCount;
    private boolean isYou;

    public GridScoreboardEntryDto(String userName, int guessedCount, int entryCount, boolean completed,
                                   boolean usedOvertime, int overtimeCount, boolean isYou) {
        this.userName = userName;
        this.guessedCount = guessedCount;
        this.entryCount = entryCount;
        this.completed = completed;
        this.usedOvertime = usedOvertime;
        this.overtimeCount = overtimeCount;
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

    public boolean isUsedOvertime() {
        return usedOvertime;
    }

    public int getOvertimeCount() {
        return overtimeCount;
    }

    // Jackson would otherwise name this property "you" (it drops the "is" from a boolean getter),
    // but the frontend's leaderboards read `isYou` - with the mismatch, a player's own row was never
    // highlighted and the "your rank" row below the top 5 never appeared.
    @JsonProperty("isYou")
    public boolean isYou() {
        return isYou;
    }
}
