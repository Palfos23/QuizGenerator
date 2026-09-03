package com.quizapp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

// One kick in a shootout's real order. Deliberately simpler than LineupEntry -
// no shirt number or captain, just where in the sequence this kick fell, which
// side took it, and whether it went in.
@Entity
@Table(name = "penalty_kicks")
public class PenaltyKick {

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

    // 1-based position in the real shootout order, interleaved across both
    // sides as it actually happened - the only hint a player gets before a
    // kick is solved (see PenaltyKickDto), matching the reference game.
    @Column(name = "kick_order", nullable = false)
    private int kickOrder;

    // true = shootout.teamName's kick, false = shootout.opponentName's.
    @Column(name = "for_team", nullable = false)
    private boolean forTeam;

    @Column(nullable = false)
    private boolean scored = true;

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

    public int getKickOrder() {
        return kickOrder;
    }

    public void setKickOrder(int kickOrder) {
        this.kickOrder = kickOrder;
    }

    public boolean isForTeam() {
        return forTeam;
    }

    public void setForTeam(boolean forTeam) {
        this.forTeam = forTeam;
    }

    public boolean isScored() {
        return scored;
    }

    public void setScored(boolean scored) {
        this.scored = scored;
    }
}
