package com.quizapp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

// The full guessable pool for a shootout's search box - every kicker plus any
// decoys the admin wants guessable (and wrong), e.g. a player subbed off
// before the shootout. Mirrors LineupCandidate exactly.
@Entity
@Table(name = "penalty_candidates")
public class PenaltyCandidate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shootout_id", nullable = false)
    @JsonIgnore
    private PenaltyShootout shootout;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "athlete_id", nullable = false)
    private Athlete athlete;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public PenaltyShootout getShootout() {
        return shootout;
    }

    public void setShootout(PenaltyShootout shootout) {
        this.shootout = shootout;
    }

    public Athlete getAthlete() {
        return athlete;
    }

    public void setAthlete(Athlete athlete) {
        this.athlete = athlete;
    }
}
