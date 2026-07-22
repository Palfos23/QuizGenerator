package com.quizapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class GridEntryInputDto {

    @NotNull
    private Long athleteId;

    @NotBlank
    private String hintLabel;

    @NotNull
    private Integer hintValue;

    public Long getAthleteId() {
        return athleteId;
    }

    public void setAthleteId(Long athleteId) {
        this.athleteId = athleteId;
    }

    public String getHintLabel() {
        return hintLabel;
    }

    public void setHintLabel(String hintLabel) {
        this.hintLabel = hintLabel;
    }

    public Integer getHintValue() {
        return hintValue;
    }

    public void setHintValue(Integer hintValue) {
        this.hintValue = hintValue;
    }
}
