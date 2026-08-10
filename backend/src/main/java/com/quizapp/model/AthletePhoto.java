package com.quizapp.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

// An additional photo for a subject, beyond its primary photoUrl - lets the
// same subject (e.g. a movie or an artist) offer several images to choose
// between, so different grids using the same subject don't have to show the
// identical picture every time.
@Entity
@Table(name = "athlete_photos")
public class AthletePhoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "athlete_id", nullable = false)
    private Athlete athlete;

    @NotBlank
    @Column(name = "photo_url", nullable = false, length = 1000)
    private String photoUrl;

    // Optional short label to tell photos apart when picking one in the grid
    // editor - e.g. "Alternate poster", "Behind the scenes". Not required.
    @Column
    private String label;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Athlete getAthlete() {
        return athlete;
    }

    public void setAthlete(Athlete athlete) {
        this.athlete = athlete;
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
