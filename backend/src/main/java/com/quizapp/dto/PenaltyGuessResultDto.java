package com.quizapp.dto;

public class PenaltyGuessResultDto {

    private boolean correct;
    private boolean allSolved;
    private PenaltyKickDto kick; // the newly revealed kick, only present when correct

    public boolean isCorrect() { return correct; }
    public void setCorrect(boolean correct) { this.correct = correct; }
    public boolean isAllSolved() { return allSolved; }
    public void setAllSolved(boolean allSolved) { this.allSolved = allSolved; }
    public PenaltyKickDto getKick() { return kick; }
    public void setKick(PenaltyKickDto kick) { this.kick = kick; }
}
