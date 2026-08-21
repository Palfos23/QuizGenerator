package com.quizapp.dto;

import jakarta.validation.constraints.NotNull;

public class BullseyeEntryInputDto {

    @NotNull
    private Long athleteId;

    @NotNull
    private Integer statValue;

    public Long getAthleteId() {
        return athleteId;
    }

    public void setAthleteId(Long athleteId) {
        this.athleteId = athleteId;
    }

    public Integer getStatValue() {
        return statValue;
    }

    public void setStatValue(Integer statValue) {
        this.statValue = statValue;
    }
}
