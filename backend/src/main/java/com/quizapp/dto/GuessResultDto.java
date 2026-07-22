package com.quizapp.dto;

public class GuessResultDto {

    private boolean correct;
    private int strikesUsed;
    private int maxStrikes;
    private boolean gameOver;
    private boolean allSolved;
    private GridEntryViewDto entry; // the newly revealed entry, only present when correct

    public boolean isCorrect() {
        return correct;
    }

    public void setCorrect(boolean correct) {
        this.correct = correct;
    }

    public int getStrikesUsed() {
        return strikesUsed;
    }

    public void setStrikesUsed(int strikesUsed) {
        this.strikesUsed = strikesUsed;
    }

    public int getMaxStrikes() {
        return maxStrikes;
    }

    public void setMaxStrikes(int maxStrikes) {
        this.maxStrikes = maxStrikes;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public void setGameOver(boolean gameOver) {
        this.gameOver = gameOver;
    }

    public boolean isAllSolved() {
        return allSolved;
    }

    public void setAllSolved(boolean allSolved) {
        this.allSolved = allSolved;
    }

    public GridEntryViewDto getEntry() {
        return entry;
    }

    public void setEntry(GridEntryViewDto entry) {
        this.entry = entry;
    }
}
