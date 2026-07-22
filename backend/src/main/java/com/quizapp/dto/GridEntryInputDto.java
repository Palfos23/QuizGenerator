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

    // Optional - which club's crest to show as an extra hint. Null = no logo shown.
    private Long clubId;

    // Defaults to true (shown) when omitted - lets an admin hide the logo for an
    // easy answer without needing to touch every other entry's payload.
    private Boolean showLogo;

    public Long getClubId() {
        return clubId;
    }

    public void setClubId(Long clubId) {
        this.clubId = clubId;
    }

    public Boolean getShowLogo() {
        return showLogo;
    }

    public void setShowLogo(Boolean showLogo) {
        this.showLogo = showLogo;
    }

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
