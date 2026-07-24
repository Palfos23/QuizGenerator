package com.quizapp.dto;

public class GridScoreboardEntryDto {

    private String userName;
    private int guessedCount;
    private int entryCount;
    private boolean completed;
    private boolean usedOvertime;
    private boolean isYou;

    public GridScoreboardEntryDto(String userName, int guessedCount, int entryCount, boolean completed,
                                   boolean usedOvertime, boolean isYou) {
        this.userName = userName;
        this.guessedCount = guessedCount;
        this.entryCount = entryCount;
        this.completed = completed;
        this.usedOvertime = usedOvertime;
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

    public boolean isYou() {
        return isYou;
    }
}
