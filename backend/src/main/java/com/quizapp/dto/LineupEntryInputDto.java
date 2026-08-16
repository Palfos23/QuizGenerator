package com.quizapp.dto;

import jakarta.validation.constraints.NotNull;

public class LineupEntryInputDto {

    @NotNull
    private Long athleteId;

    @NotNull
    private Integer shirtNumber;

    @NotNull
    private Integer slotIndex;

    private boolean captain = false;

    public Long getAthleteId() { return athleteId; }
    public void setAthleteId(Long athleteId) { this.athleteId = athleteId; }
    public Integer getShirtNumber() { return shirtNumber; }
    public void setShirtNumber(Integer shirtNumber) { this.shirtNumber = shirtNumber; }
    public Integer getSlotIndex() { return slotIndex; }
    public void setSlotIndex(Integer slotIndex) { this.slotIndex = slotIndex; }
    public boolean isCaptain() { return captain; }
    public void setCaptain(boolean captain) { this.captain = captain; }
}
