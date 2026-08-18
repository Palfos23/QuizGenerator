package com.quizapp.model;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

// Persisted per-user progress against a solo (weekly) Starting XI board - the
// same purpose GridAttempt serves for Grid. Deliberately has no "overtime"
// concept, unlike GridAttempt, since Starting XI has no continue-after-fail
// mode: once completed you can only give up and reveal, or you're done.
@Entity
@Table(name = "lineup_attempts", uniqueConstraints = @UniqueConstraint(columnNames = {"lineup_id", "user_id"}))
public class LineupAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lineup_id", nullable = false)
    private Lineup lineup;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(name = "strikes_used", nullable = false)
    private int strikesUsed = 0;

    @Column(nullable = false)
    private boolean completed = false;

    // True if the player gave up and had the remaining slots revealed, rather
    // than solving everything or running out of lives naturally.
    @Column(nullable = false)
    private boolean revealed = false;

    // Same purpose as GridAttempt.includeOnLeaderboard - defaults to visible.
    @Column(name = "include_on_leaderboard", nullable = false)
    private boolean includeOnLeaderboard = true;

    @ElementCollection
    @CollectionTable(name = "lineup_attempt_solved_entries", joinColumns = @JoinColumn(name = "attempt_id"))
    @Column(name = "lineup_entry_id")
    private Set<Long> solvedEntryIds = new HashSet<>();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

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

    public AppUser getUser() {
        return user;
    }

    public void setUser(AppUser user) {
        this.user = user;
    }

    public int getStrikesUsed() {
        return strikesUsed;
    }

    public void setStrikesUsed(int strikesUsed) {
        this.strikesUsed = strikesUsed;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public boolean isRevealed() {
        return revealed;
    }

    public void setRevealed(boolean revealed) {
        this.revealed = revealed;
    }

    public boolean isIncludeOnLeaderboard() {
        return includeOnLeaderboard;
    }

    public void setIncludeOnLeaderboard(boolean includeOnLeaderboard) {
        this.includeOnLeaderboard = includeOnLeaderboard;
    }

    public Set<Long> getSolvedEntryIds() {
        return solvedEntryIds;
    }

    public void setSolvedEntryIds(Set<Long> solvedEntryIds) {
        this.solvedEntryIds = solvedEntryIds;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
