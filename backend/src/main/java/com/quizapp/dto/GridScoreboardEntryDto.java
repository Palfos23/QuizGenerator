package com.quizapp.dto;

public class GridScoreboardEntryDto {

    private String userName;
    private int guessedCount;
    private int entryCount;
    private boolean completed;
    private boolean usedOvertime;

    public GridScoreboardEntryDto(String userName, int guessedCount, int entryCount, boolean completed, boolean usedOvertime) {
        this.userName = userName;
        this.guessedCount = guessedCount;
        this.entryCount = entryCount;
        this.completed = completed;
        this.usedOvertime = usedOvertime;
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
}
