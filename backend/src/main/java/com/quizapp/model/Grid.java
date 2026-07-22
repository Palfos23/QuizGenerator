package com.quizapp.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "grids")
public class Grid {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String title;

    // Longer instructions, e.g. "Name every Tottenham player with more than 10 goals."
    @Column(length = 1000)
    private String theme;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Sport sport;

    // The Monday this grid is "live" for. Active window is [weekStartDate, weekStartDate + 6 days].
    @NotNull
    @Column(name = "week_start_date", nullable = false)
    private LocalDate weekStartDate;

    @Column(name = "max_strikes", nullable = false)
    private int maxStrikes = 5;

    // The full searchable pool for this grid's guess box - includes both the correct
    // answers (see GridEntry) and any decoys the admin wants users to be able to guess
    // (and get wrong) - e.g. every other Spurs player who didn't hit 10 goals.
    @OneToMany(mappedBy = "grid", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<GridCandidate> candidates = new ArrayList<>();

    // The correct answers - a subset of candidates, each annotated with the hint
    // shown on its tile before it's solved (e.g. "FW" / 14).
    @OneToMany(mappedBy = "grid", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<GridEntry> entries = new ArrayList<>();

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

    public String getTheme() {
        return theme;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }

    public Sport getSport() {
        return sport;
    }

    public void setSport(Sport sport) {
        this.sport = sport;
    }

    public LocalDate getWeekStartDate() {
        return weekStartDate;
    }

    public void setWeekStartDate(LocalDate weekStartDate) {
        this.weekStartDate = weekStartDate;
    }

    public int getMaxStrikes() {
        return maxStrikes;
    }

    public void setMaxStrikes(int maxStrikes) {
        this.maxStrikes = maxStrikes;
    }

    public List<GridCandidate> getCandidates() {
        return candidates;
    }

    public void setCandidates(List<GridCandidate> candidates) {
        this.candidates.clear();
        if (candidates != null) {
            for (GridCandidate c : candidates) {
                c.setGrid(this);
                this.candidates.add(c);
            }
        }
    }

    public List<GridEntry> getEntries() {
        return entries;
    }

    public void setEntries(List<GridEntry> entries) {
        this.entries.clear();
        if (entries != null) {
            for (GridEntry e : entries) {
                e.setGrid(this);
                this.entries.add(e);
            }
        }
    }
}
