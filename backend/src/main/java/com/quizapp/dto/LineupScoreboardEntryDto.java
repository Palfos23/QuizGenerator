package com.quizapp.dto;

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

    public boolean isYou() {
        return isYou;
    }
}
