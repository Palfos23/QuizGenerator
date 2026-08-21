package com.quizapp.dto;

// The lean shape sent to players once per round - just enough to match a
// submitted name and look up its real stat value. No secrecy to protect here
// (the whole answer key is fair game the moment the round starts), unlike
// GridEntryViewDto which withholds the answer until solved.
public class BullseyeEntryViewDto {

    private Long athleteId;
    private String athleteName;
    private Integer statValue;

    public BullseyeEntryViewDto(Long athleteId, String athleteName, Integer statValue) {
        this.athleteId = athleteId;
        this.athleteName = athleteName;
        this.statValue = statValue;
    }

    public Long getAthleteId() {
        return athleteId;
    }

    public String getAthleteName() {
        return athleteName;
    }

    public Integer getStatValue() {
        return statValue;
    }
}
