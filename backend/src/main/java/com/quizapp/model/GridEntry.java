package com.quizapp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "grid_entries")
public class GridEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grid_id", nullable = false)
    @JsonIgnore
    private Grid grid;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "athlete_id", nullable = false)
    private Athlete athlete;

    // Short label shown on the tile, e.g. "FW", "WR", "GC" (general classification) - admin's choice.
    @NotBlank
    @Column(name = "hint_label", nullable = false)
    private String hintLabel;

    // The number shown alongside the label, e.g. goals scored, races won.
    @Column(name = "hint_value", nullable = false)
    private int hintValue;

    // Display order for the tiles - the reference game sorts by hint value descending.
    @Column(name = "order_index", nullable = false)
    private int orderIndex;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Grid getGrid() {
        return grid;
    }

    public void setGrid(Grid grid) {
        this.grid = grid;
    }

    public Athlete getAthlete() {
        return athlete;
    }

    public void setAthlete(Athlete athlete) {
        this.athlete = athlete;
    }

    public String getHintLabel() {
        return hintLabel;
    }

    public void setHintLabel(String hintLabel) {
        this.hintLabel = hintLabel;
    }

    public int getHintValue() {
        return hintValue;
    }

    public void setHintValue(int hintValue) {
        this.hintValue = hintValue;
    }

    public int getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(int orderIndex) {
        this.orderIndex = orderIndex;
    }
}
