package com.quizapp.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

// An additional text description/quote for a subject (e.g. a line from a
// book) - mirrors AthletePhoto's pattern exactly, letting the same subject
// offer several quotes to choose between so different grids using the same
// subject don't have to show the identical one every time.
@Entity
@Table(name = "athlete_descriptions")
public class AthleteDescription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "athlete_id", nullable = false)
    private Athlete athlete;

    @NotBlank
    @Column(name = "text", nullable = false, length = 2000)
    private String text;

    // Optional short label to tell descriptions apart when picking one in
    // the grid editor - e.g. "Opening line", "Famous quote". Not required.
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

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }
}
