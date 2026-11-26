package com.quizapp.model;

import jakarta.persistence.*;

import java.time.Instant;

/** A player in a league (past members are kept, with leftAt set). The name is always looked up live (AppUser), so a renamed player reads right everywhere. */
@Entity
@Table(name = "league_members", uniqueConstraints = @UniqueConstraint(columnNames = {"league_id", "user_id"}))
public class LeagueMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "league_id")
    private League league;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt = Instant.now();

    // Set when the player leaves or is removed. The row is kept, so the seasons they took part in still list
    // them (a points table must not change after the fact); cleared again if they rejoin.
    @Column(name = "left_at")
    private Instant leftAt;

    public LeagueMember() {
    }

    public LeagueMember(League league, Long userId) {
        this.league = league;
        this.userId = userId;
    }

    public Long getId() { return id; }
    public League getLeague() { return league; }
    public Long getUserId() { return userId; }
    public Instant getJoinedAt() { return joinedAt; }
    public Instant getLeftAt() { return leftAt; }
    public void setLeftAt(Instant leftAt) { this.leftAt = leftAt; }
    public boolean isActive() { return leftAt == null; }
}
