package com.quizapp.model;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * A private daily-quiz league: a named group of players who compete on the same daily quiz as everyone
 * else, in seasons (LeagueSeason) with a format of their choosing. Everyone is also in the global league,
 * which is not stored - it is simply the daily quiz's own leaderboard.
 *
 * Invited by code (like a game room): /liga/CODE. Users are referred to by id with no foreign key, so a
 * league's history survives a member leaving or deleting their account (they then show as "Slettet spiller").
 */
@Entity
@Table(name = "leagues")
public class League {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 40)
    private String name;

    @Column(nullable = false, unique = true, length = 8)
    private String code;

    @Column(name = "owner_user_id", nullable = false)
    private Long ownerUserId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public Long getOwnerUserId() { return ownerUserId; }
    public void setOwnerUserId(Long ownerUserId) { this.ownerUserId = ownerUserId; }
    public Instant getCreatedAt() { return createdAt; }
}
