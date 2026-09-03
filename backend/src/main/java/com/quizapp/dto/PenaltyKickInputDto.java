package com.quizapp.dto;

import jakarta.validation.constraints.NotNull;

public class PenaltyKickInputDto {

    @NotNull
    private Long athleteId;

    @NotNull
    private Integer kickOrder;

    private boolean forTeam = true;
    private boolean scored = true;

    public Long getAthleteId() { return athleteId; }
    public void setAthleteId(Long athleteId) { this.athleteId = athleteId; }
    public Integer getKickOrder() { return kickOrder; }
    public void setKickOrder(Integer kickOrder) { this.kickOrder = kickOrder; }
    public boolean isForTeam() { return forTeam; }
    public void setForTeam(boolean forTeam) { this.forTeam = forTeam; }
    public boolean isScored() { return scored; }
    public void setScored(boolean scored) { this.scored = scored; }
}
