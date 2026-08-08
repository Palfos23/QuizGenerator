package com.quizapp.dto;

import jakarta.validation.constraints.NotNull;

public class GridEntryInputDto {

    @NotNull
    private Long athleteId;

    // Optional - an admin can leave this blank if just the value is enough
    // (e.g. some hints are too easy to guess with a label attached).
    private String hintLabel;

    // Required when the grid is ranked, null when it isn't - validated in
    // GridAdminService rather than here, since the requirement depends on
    // the parent grid's own ranked flag.
    private Integer hintValue;

    // Optional - which club's crest to show as an extra hint. Null = no logo shown.
    private Long clubId;

    // Defaults to true (shown) when omitted - lets an admin hide the logo for an
    // easy answer without needing to touch every other entry's payload.
    private Boolean showLogo;

    // When true, use this entry's own subject photo as the hint-badge image
    // instead of a club's crest - for content where every entry's image is
    // unique to itself (movie posters), not shared across entries.
    private boolean useOwnPhotoAsLogo = false;

    public boolean isUseOwnPhotoAsLogo() {
        return useOwnPhotoAsLogo;
    }

    public void setUseOwnPhotoAsLogo(boolean useOwnPhotoAsLogo) {
        this.useOwnPhotoAsLogo = useOwnPhotoAsLogo;
    }

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
