package com.quizapp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

// One of the 11 correct starters on a Lineup board, pinned to a specific
// formation slot. Deliberately simpler than GridEntry - no club/logo/photo
// selection or description reveal mode, since a lineup always shows the
// athlete's own name and photo once guessed.
@Entity
@Table(name = "lineup_entries")
public class LineupEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lineup_id", nullable = false)
    @JsonIgnore
    private Lineup lineup;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "athlete_id", nullable = false)
    private Athlete athlete;

    // Shirt number shown on the tile before it's solved - the only hint a
    // player gets, matching the reference game.
    @Column(name = "shirt_number", nullable = false)
    private int shirtNumber;

    // Position within the formation, 0-based, ordered goalkeeper first then
    // outfield rows in ascending order (see frontend/src/services/formations.js
    // for how a formation string like "4-3-3" maps slot indexes to rows/columns).
    @Column(name = "slot_index", nullable = false)
    private int slotIndex;

    @Column(nullable = false)
    private boolean captain = false;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Lineup getLineup() {
        return lineup;
    }

    public void setLineup(Lineup lineup) {
        this.lineup = lineup;
    }

    public Athlete getAthlete() {
        return athlete;
    }

    public void setAthlete(Athlete athlete) {
        this.athlete = athlete;
    }

    public int getShirtNumber() {
        return shirtNumber;
    }

    public void setShirtNumber(int shirtNumber) {
        this.shirtNumber = shirtNumber;
    }

    public int getSlotIndex() {
        return slotIndex;
    }

    public void setSlotIndex(int slotIndex) {
        this.slotIndex = slotIndex;
    }

    public boolean isCaptain() {
        return captain;
    }

    public void setCaptain(boolean captain) {
        this.captain = captain;
    }
}
