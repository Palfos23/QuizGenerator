package com.quizapp.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "clubs")
public class Club {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String name;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Sport sport;

    // Plain URL to a hosted crest image - not a file upload (Render's filesystem is
    // ephemeral, so storing uploaded files locally would be lost on every redeploy).
    @Column(name = "logo_url", length = 1000)
    private String logoUrl;

    // Hex color (e.g. "#F2B705") used for this club's hint badge on grid tiles, so a
    // badge doesn't have to be the same gold for every club regardless of team colors.
    // Nullable - falls back to the app's default gold wherever it's read.
    @Column(name = "color", length = 20)
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
