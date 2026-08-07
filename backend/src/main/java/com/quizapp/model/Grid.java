package com.quizapp.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

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

    @NotBlank
    @Column(nullable = false)
    private String sport;

    // The Monday this grid is "live" for. Active window is [weekStartDate, weekStartDate + 6 days].
    @NotNull
    @Column(name = "week_start_date", nullable = false)
    private LocalDate weekStartDate;

    @Column(name = "max_strikes", nullable = false)
    private int maxStrikes = 5;

    // Most grids rank by "biggest number wins" (goals, appearances) so tiles sort
    // highest-value-first by default. A grid themed around finishing position
    // (e.g. "Top 20 riders, Tour de France 2025") needs the opposite - 1st place
    // should lead, not trail - so this lets an admin flip the sort direction.
    @Column(name = "sort_ascending", nullable = false)
    private boolean sortAscending = false;

    // For when a grid needs updated content (e.g. a player crosses a stat
    // threshold) - rather than editing the original in place (which would
    // retroactively change what "complete" means for anyone who already
    // played it), the intended workflow is to duplicate it, edit the copy,
    // and mark the original excluded here. It stays fully intact and playable
    // on the regular Weekly Grid page - this only removes it from Grid
    // Battle's random/manual pick pool, since that's where stale content
    // would actually matter.
    @Column(name = "excluded_from_grid_battle", nullable = false)
    private boolean excludedFromGridBattle = false;

    public boolean isExcludedFromGridBattle() {
        return excludedFromGridBattle;
    }

    public void setExcludedFromGridBattle(boolean excludedFromGridBattle) {
        this.excludedFromGridBattle = excludedFromGridBattle;
    }

    public boolean isSortAscending() {
        return sortAscending;
    }

    public void setSortAscending(boolean sortAscending) {
        this.sortAscending = sortAscending;
    }

    // The full searchable pool for this grid's guess box - includes both the correct
    // answers (see GridEntry) and any decoys the admin wants users to be able to guess
    // (and get wrong) - e.g. every other Spurs player who didn't hit 10 goals.
    //
    // Set, not List: Hibernate maps a List-mapped @OneToMany as an unordered "bag",
    // which has no reliable way to diff removals - in some cases it issues an UPDATE
    // nulling out the foreign key instead of a clean DELETE, which fails outright on
    // a NOT NULL column. A Set sidesteps this entirely, using proper equality-based
    // diffing instead of Hibernate's ambiguous bag-position tracking. Order was never
    // meaningful for this collection anyway.
    @OneToMany(mappedBy = "grid", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<GridCandidate> candidates = new HashSet<>();

    // The correct answers - a subset of candidates, each annotated with the hint
    // shown on its tile before it's solved (e.g. "FW" / 14).
    // Same Set-not-List reasoning as candidates above. Display order is unrelated -
    // it's always driven by an explicit sort at read time (see entrySortOrder in
    // GridPlayService), never by this collection's own iteration order.
    @OneToMany(mappedBy = "grid", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private Set<GridEntry> entries = new HashSet<>();

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

    public String getSport() {
        return sport;
    }

    public void setSport(String sport) {
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

    public Set<GridCandidate> getCandidates() {
        return candidates;
    }

    public void setCandidates(Set<GridCandidate> candidates) {
        Set<GridCandidate> incoming = candidates != null ? candidates : new HashSet<>();
        this.candidates.removeIf(existing -> !incoming.contains(existing));
        for (GridCandidate c : incoming) {
            if (!this.candidates.contains(c)) {
                c.setGrid(this);
                this.candidates.add(c);
            }
        }
    }

    public Set<GridEntry> getEntries() {
        return entries;
    }

    public void setEntries(Set<GridEntry> entries) {
        Set<GridEntry> incoming = entries != null ? entries : new HashSet<>();
        this.entries.removeIf(existing -> !incoming.contains(existing));
        for (GridEntry e : incoming) {
            if (!this.entries.contains(e)) {
                e.setGrid(this);
                this.entries.add(e);
            }
        }
    }
}
