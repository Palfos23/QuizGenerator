package com.quizapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class AthleteDto {

    private Long id;

    @NotBlank
    private String name;

    @NotNull
    private String sport;

    private String team;

    private String photoUrl;

    // Additional photos beyond the primary one - a grid can pick any of
    // these (or the primary photoUrl) to show for this subject specifically.
    private java.util.List<AthletePhotoDto> additionalPhotos = new java.util.ArrayList<>();

    public java.util.List<AthletePhotoDto> getAdditionalPhotos() {
        return additionalPhotos;
    }

    public void setAdditionalPhotos(java.util.List<AthletePhotoDto> additionalPhotos) {
        this.additionalPhotos = additionalPhotos;
    }

    // Quotes/descriptions for this subject - a grid can pick one to show
    // instead of a photo once an entry is solved.
    private java.util.List<AthleteDescriptionDto> additionalDescriptions = new java.util.ArrayList<>();

    public java.util.List<AthleteDescriptionDto> getAdditionalDescriptions() {
        return additionalDescriptions;
    }

    public void setAdditionalDescriptions(java.util.List<AthleteDescriptionDto> additionalDescriptions) {
        this.additionalDescriptions = additionalDescriptions;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSport() {
        return sport;
    }

    public void setSport(String sport) {
        this.sport = sport;
    }

    public String getTeam() {
        return team;
    }

    public void setTeam(String team) {
        this.team = team;
    }
}
