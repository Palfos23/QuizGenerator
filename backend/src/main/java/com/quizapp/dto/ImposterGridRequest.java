package com.quizapp.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class ImposterGridRequest {

    @NotBlank
    private String title;

    private String description;

    @NotBlank
    private String sport;

    @NotBlank
    private String displayMode; // NAME_AND_LOGO, NAME_AND_PHOTO, PHOTO_ONLY

    @NotEmpty
    @Valid
    private List<TileInput> tiles;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSport() {
        return sport;
    }

    public void setSport(String sport) {
        this.sport = sport;
    }

    public String getDisplayMode() {
        return displayMode;
    }

    public void setDisplayMode(String displayMode) {
        this.displayMode = displayMode;
    }

    public List<TileInput> getTiles() {
        return tiles;
    }

    public void setTiles(List<TileInput> tiles) {
        this.tiles = tiles;
    }

    public static class TileInput {
        @NotNull
        private Long athleteId;
        private boolean imposter;
        private Long replacedAthleteId; // only meaningful when imposter=true
        private Long clubId; // only meaningful when the grid's displayMode is NAME_AND_LOGO
        private Long selectedPhotoId; // which of the subject's additional photos to show, if any

        public Long getSelectedPhotoId() {
            return selectedPhotoId;
        }

        public void setSelectedPhotoId(Long selectedPhotoId) {
            this.selectedPhotoId = selectedPhotoId;
        }

        public Long getClubId() {
            return clubId;
        }

        public void setClubId(Long clubId) {
            this.clubId = clubId;
        }

        public Long getAthleteId() {
            return athleteId;
        }

        public void setAthleteId(Long athleteId) {
            this.athleteId = athleteId;
        }

        public boolean isImposter() {
            return imposter;
        }

        public void setImposter(boolean imposter) {
            this.imposter = imposter;
        }

        public Long getReplacedAthleteId() {
            return replacedAthleteId;
        }

        public void setReplacedAthleteId(Long replacedAthleteId) {
            this.replacedAthleteId = replacedAthleteId;
        }
    }
}
