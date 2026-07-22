package com.quizapp.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "athletes")
public class Athlete {

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

    // Free text: club name for football, team name for cycling. Used both to help
    // an admin quickly find "everyone on team X" and shown alongside the name in
    // the guess search box so users can tell athletes with the same name apart.
    @Column
    private String team;

    // A hosted image URL, same tradeoff as Club.logoUrl - not a file upload, since
    // Render's filesystem is ephemeral. Only ever exposed to players once a tile is
    // solved (see GridPlayService) - showing it earlier would spoil the guess.
    @Column(name = "photo_url", length = 1000)
    private String photoUrl;

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

    public Sport getSport() {
        return sport;
    }

    public void setSport(Sport sport) {
        this.sport = sport;
    }

    public String getTeam() {
        return team;
    }

    public void setTeam(String team) {
        this.team = team;
    }
}
