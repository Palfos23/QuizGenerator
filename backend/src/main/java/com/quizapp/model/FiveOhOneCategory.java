package com.quizapp.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "five_oh_one_categories")
public class FiveOhOneCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String title;

    @Column(length = 500)
    private String description;

    // Optional (existing categories predate this field, so it's nullable, not
    // required) - lets the admin editor check CSV-imported names against the
    // Subjects/Athlete pool for this sport, the same way Bullseye's importer
    // does, and offer to add any that aren't in it yet. Purely an admin-side
    // convenience; entries stay free text either way (see FiveOhOneEntry).
    @Column
    private String sport;

    public String getSport() {
        return sport;
    }

    public void setSport(String sport) {
        this.sport = sport;
    }

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    // Same purpose as Grid.updatedAt - see that field for the full reasoning.
    @Column(name = "updated_at")
    private Instant updatedAt;

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    // Flags this category's content as time-sensitive (e.g. "current all-time top
    // scorer") so it surfaces on the admin "Can expire" review page for periodic
    // rechecking. Purely an admin bookkeeping signal - doesn't affect visibility or
    // behavior anywhere else. See Grid.canExpire for the same field on another game.
    @Column(name = "can_expire", nullable = false, columnDefinition = "boolean default false")
    private boolean canExpire = false;

    public boolean isCanExpire() {
        return canExpire;
    }

    public void setCanExpire(boolean canExpire) {
        this.canExpire = canExpire;
    }

    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<FiveOhOneEntry> entries = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public List<FiveOhOneEntry> getEntries() {
        return entries;
    }

    public void setEntries(List<FiveOhOneEntry> entries) {
        this.entries.clear();
        if (entries != null) {
            for (FiveOhOneEntry e : entries) {
                e.setCategory(this);
                this.entries.add(e);
            }
        }
    }
}
