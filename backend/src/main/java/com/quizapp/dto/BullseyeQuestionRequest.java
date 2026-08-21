package com.quizapp.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class BullseyeQuestionRequest {

    @NotBlank
    private String title;

    @NotNull
    private String sport;

    @NotNull
    private Integer targetValue;

    @NotBlank
    private String statLabel;

    private boolean excludedFromBullseye = false;

    @NotEmpty
    @Valid
    private List<BullseyeEntryInputDto> entries;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSport() {
        return sport;
    }

    public void setSport(String sport) {
        this.sport = sport;
    }

    public Integer getTargetValue() {
        return targetValue;
    }

    public void setTargetValue(Integer targetValue) {
        this.targetValue = targetValue;
    }

    public String getStatLabel() {
        return statLabel;
    }

    public void setStatLabel(String statLabel) {
        this.statLabel = statLabel;
    }

    public boolean isExcludedFromBullseye() {
        return excludedFromBullseye;
    }

    public void setExcludedFromBullseye(boolean excludedFromBullseye) {
        this.excludedFromBullseye = excludedFromBullseye;
    }

    public List<BullseyeEntryInputDto> getEntries() {
        return entries;
    }

    public void setEntries(List<BullseyeEntryInputDto> entries) {
        this.entries = entries;
    }
}
