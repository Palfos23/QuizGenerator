package com.quizapp.dto;

import java.util.List;

// One cluster of subjects (athletes) within a single sport that the data-quality
// scan thinks might be the same person registered more than once - see
// AthleteService#findDuplicateGroups for the actual detection heuristics.
public class AthleteDuplicateGroupDto {

    private String sport;
    private String reason;
    private List<AthleteDto> athletes;

    public AthleteDuplicateGroupDto(String sport, String reason, List<AthleteDto> athletes) {
        this.sport = sport;
        this.reason = reason;
        this.athletes = athletes;
    }

    public String getSport() {
        return sport;
    }

    public String getReason() {
        return reason;
    }

    public List<AthleteDto> getAthletes() {
        return athletes;
    }
}
