package com.quizapp.dto;

import jakarta.validation.constraints.NotBlank;

public class AthletePhotoDto {

    private Long id;

    @NotBlank
    private String photoUrl;

    private String label;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }
}
