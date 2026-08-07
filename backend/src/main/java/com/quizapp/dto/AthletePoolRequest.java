package com.quizapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class AthletePoolRequest {
    @NotBlank
    private String name;

    @NotNull
    private String sport;

    @NotEmpty
    private List<Long> athleteIds;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSport() { return sport; }
    public void setSport(String sport) { this.sport = sport; }
    public List<Long> getAthleteIds() { return athleteIds; }
    public void setAthleteIds(List<Long> athleteIds) { this.athleteIds = athleteIds; }
}
