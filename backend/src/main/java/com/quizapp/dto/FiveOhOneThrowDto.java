package com.quizapp.dto;

public class FiveOhOneThrowDto {
    private String playerName;
    private String entryName;
    private int rawValue;
    private int score;
    private boolean bust;
    private int resultingTotal;

    public FiveOhOneThrowDto(String playerName, String entryName, int rawValue, int score, boolean bust, int resultingTotal) {
        this.playerName = playerName;
        this.entryName = entryName;
        this.rawValue = rawValue;
        this.score = score;
        this.bust = bust;
        this.resultingTotal = resultingTotal;
    }

    public String getPlayerName() { return playerName; }
    public String getEntryName() { return entryName; }
    public int getRawValue() { return rawValue; }
    public int getScore() { return score; }
    public boolean isBust() { return bust; }
    public int getResultingTotal() { return resultingTotal; }
}
