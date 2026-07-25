package com.quizapp.dto;

import jakarta.validation.constraints.NotNull;

public class GridBattleGuessRequest {
    @NotNull
    private Long athleteId;

    public Long getAthleteId() { return athleteId; }
    public void setAthleteId(Long athleteId) { this.athleteId = athleteId; }
}
