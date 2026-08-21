package com.quizapp.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "bullseye_questions")
public class BullseyeQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Admin-internal label - never shown to players.
    @NotBlank
    @Column(nullable = false)
    private String title;

    // Category, same free-text field/convention as Grid.sport - validated
    // against the admin-managed GridCategory list at the app layer.
    @NotBlank
    @Column(nullable = false)
    private String sport;

    // The number players are trying to get closest to, e.g. 13.
    @NotNull
    @Column(name = "target_value", nullable = false)
    private Integer targetValue;

    // The rest of the prompt, e.g. "goals in the Premier League 2024/25" -
    // rendered to players as "{targetValue} {statLabel}".
    @NotBlank
    @Column(name = "stat_label", nullable = false)
    private String statLabel;

    // Same purpose as Grid.excludedFromGridBattle - retires a stale question
    // from the random/manual pick pool without deleting it.
    @Column(name = "excluded_from_bullseye", nullable = false)
    private boolean excludedFromBullseye = false;

    public boolean isExcludedFromBullseye() {
        return excludedFromBullseye;
    }

    public void setExcludedFromBullseye(boolean excludedFromBullseye) {
        this.excludedFromBullseye = excludedFromBullseye;
    }

    // The authored answer pool: every athlete a player could name, each with
    // its own real stat value for this specific question. Set, not List -
    // same Hibernate bag-diffing reasoning as Grid.entries.
    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private Set<BullseyeEntry> entries = new HashSet<>();

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

    public String getSport() {
        return sport;
    }

    public void setSport(String sport) {
        this.sport = sport;
    }

    public Integer getTargetValue() {
        return targetValue;
    }

    public void setTargetValue(Integer targetValue) {
        this.targetValue = targetValue;
    }

    public String getStatLabel() {
        return statLabel;
    }

    public void setStatLabel(String statLabel) {
        this.statLabel = statLabel;
    }

    public Set<BullseyeEntry> getEntries() {
        return entries;
    }

    public void setEntries(Set<BullseyeEntry> entries) {
        Set<BullseyeEntry> incoming = entries != null ? entries : new HashSet<>();
        this.entries.removeIf(existing -> !incoming.contains(existing));
        for (BullseyeEntry e : incoming) {
            if (!this.entries.contains(e)) {
                e.setQuestion(this);
                this.entries.add(e);
            }
        }
    }
}
