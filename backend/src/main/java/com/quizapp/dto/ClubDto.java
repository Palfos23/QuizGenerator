package com.quizapp.dto;

import com.quizapp.model.Sport;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class ClubDto {

    private Long id;

    @NotBlank
    private String name;

    @NotNull
    private Sport sport;

    private String logoUrl;

    private String color;

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
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

    public Sport getSport() {
        return sport;
    }

    public void setSport(Sport sport) {
        this.sport = sport;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }
}
