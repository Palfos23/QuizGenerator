package com.quizapp.model;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "grid_attempts", uniqueConstraints = @UniqueConstraint(columnNames = {"grid_id", "user_id"}))
public class GridAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "grid_id", nullable = false)
    private Grid grid;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(name = "strikes_used", nullable = false)
    private int strikesUsed = 0;

    @Column(nullable = false)
    private boolean completed = false;

    // True once the player has chosen to keep guessing after running out of strikes -
    // further correct guesses still reveal tiles, but nothing here counts toward score.
    @Column(nullable = false)
    private boolean overtime = false;

    // True if the player gave up and had the remaining answers revealed, rather than
    // solving everything or running out of strikes naturally.
    @Column(nullable = false)
    private boolean revealed = false;

    @ElementCollection
    @CollectionTable(name = "grid_attempt_solved_entries", joinColumns = @JoinColumn(name = "attempt_id"))
    @Column(name = "grid_entry_id")
    private Set<Long> solvedEntryIds = new HashSet<>();

    // Subset of solvedEntryIds that were specifically solved while overtime was
    // active - lets the UI show "found, but during overtime" distinctly from a
    // normal timed solve, rather than treating every green tile the same way.
    @ElementCollection
    @CollectionTable(name = "grid_attempt_overtime_entries", joinColumns = @JoinColumn(name = "attempt_id"))
    @Column(name = "grid_entry_id")
    private Set<Long> overtimeSolvedEntryIds = new HashSet<>();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

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

    public boolean isOvertime() {
        return overtime;
    }

    public void setOvertime(boolean overtime) {
        this.overtime = overtime;
    }

    public boolean isRevealed() {
        return revealed;
    }

    public void setRevealed(boolean revealed) {
        this.revealed = revealed;
    }

    public Set<Long> getSolvedEntryIds() {
        return solvedEntryIds;
    }

    public void setSolvedEntryIds(Set<Long> solvedEntryIds) {
        this.solvedEntryIds = solvedEntryIds;
    }

    public Set<Long> getOvertimeSolvedEntryIds() {
        return overtimeSolvedEntryIds;
    }

    public void setOvertimeSolvedEntryIds(Set<Long> overtimeSolvedEntryIds) {
        this.overtimeSolvedEntryIds = overtimeSolvedEntryIds;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
