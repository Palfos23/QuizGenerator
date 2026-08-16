package com.quizapp.dto;

public class LineupGuessResultDto {

    private boolean correct;
    private boolean allSolved;
    private LineupSlotDto slot; // the newly revealed slot, only present when correct

    public boolean isCorrect() { return correct; }
    public void setCorrect(boolean correct) { this.correct = correct; }
    public boolean isAllSolved() { return allSolved; }
    public void setAllSolved(boolean allSolved) { this.allSolved = allSolved; }
    public LineupSlotDto getSlot() { return slot; }
    public void setSlot(LineupSlotDto slot) { this.slot = slot; }
}
