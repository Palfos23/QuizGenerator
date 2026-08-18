package com.quizapp.dto;

public class LineupGuessResultDto {

    private boolean correct;
    private boolean allSolved;
    private LineupSlotDto slot; // the newly revealed slot, only present when correct

    // Only meaningful for the persisted solo path - the stateless multiplayer
    // path leaves these at their default 0/false, since strikes there are
    // tracked entirely client-side (see MultiplayerLineupGame.vue's own
    // livesUsed ref).
    private int strikesUsed;
    private int maxStrikes;
    private boolean gameOver;

    public boolean isCorrect() { return correct; }
    public void setCorrect(boolean correct) { this.correct = correct; }
    public boolean isAllSolved() { return allSolved; }
    public void setAllSolved(boolean allSolved) { this.allSolved = allSolved; }
    public LineupSlotDto getSlot() { return slot; }
    public void setSlot(LineupSlotDto slot) { this.slot = slot; }
    public int getStrikesUsed() { return strikesUsed; }
    public void setStrikesUsed(int strikesUsed) { this.strikesUsed = strikesUsed; }
    public int getMaxStrikes() { return maxStrikes; }
    public void setMaxStrikes(int maxStrikes) { this.maxStrikes = maxStrikes; }
    public boolean isGameOver() { return gameOver; }
    public void setGameOver(boolean gameOver) { this.gameOver = gameOver; }
}
