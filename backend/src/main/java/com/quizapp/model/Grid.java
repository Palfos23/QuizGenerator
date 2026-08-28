package com.quizapp.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
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

    // Set to "now" every time an admin saves this grid (see GridAdminService)
    // - shown to players so stale content (a stat that's since changed, a
    // retired player still on the board) reads as "might be outdated" rather
    // than as a bug. Deliberately a plain field set explicitly in the service
    // rather than a @PreUpdate hook, since editing just the candidate/entry
    // rows (a very common edit) doesn't dirty this entity's own columns and
    // wouldn't reliably fire one.
    @Column(name = "updated_at")
    private Instant updatedAt;

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    // Most grids rank by "biggest number wins" (goals, appearances) so tiles sort
    // highest-value-first by default. A grid themed around finishing position
    // (e.g. "Top 20 riders, Tour de France 2025") needs the opposite - 1st place
    // should lead, not trail - so this lets an admin flip the sort direction.
    @Column(name = "sort_ascending", nullable = false)
    private boolean sortAscending = false;

    // A grid can skip ranking entirely - e.g. movie covers with no numeric
    // score at all, where the image itself is the hint rather than something
    // revealed after a separate abstract hint. Defaults to true so every
    // existing grid keeps behaving exactly as it always has; sortAscending is
    // simply ignored when this is false.
    @Column(nullable = false)
    private boolean ranked = true;

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

    // When true, this grid's guessable pool is never stored as explicit
    // GridCandidate rows at all - GridPlayService.searchCandidates() instead
    // queries every Athlete in this grid's own "sport" category live. That
    // keeps the pool always current (a subject added to the category tomorrow
    // is immediately guessable here, no re-import needed) and avoids writing
    // one redundant candidate row per athlete per grid for what's otherwise
    // pure decoy padding with zero grid-specific data attached to it. Correct
    // answers still live in "entries" exactly as before either way -
    // columnDefinition carries an explicit DB-level default so this can be
    // added to an already-populated "grids" table via ddl-auto=update without
    // the silent failure that hits a plain nullable=false boolean column.
    @Column(name = "entire_category_pool", nullable = false, columnDefinition = "boolean default false")
    private boolean entireCategoryPool = false;

    public boolean isEntireCategoryPool() {
        return entireCategoryPool;
    }

    public void setEntireCategoryPool(boolean entireCategoryPool) {
        this.entireCategoryPool = entireCategoryPool;
    }

    // What a solved entry shows: its subject's photo (the long-standing
    // default) or a text quote/description instead - e.g. a line from a book,
    // for grids about books rather than people with photos.
    public enum RevealMode {
        PHOTO, DESCRIPTION
    }

    @Enumerated(EnumType.STRING)
    @Column(name = "reveal_mode", nullable = false)
    private RevealMode revealMode = RevealMode.PHOTO;

    public RevealMode getRevealMode() {
        return revealMode;
    }

    public void setRevealMode(RevealMode revealMode) {
        this.revealMode = revealMode;
    }

    // How a solved tile's image is fitted to the (square) tile. Default false =
    // the long-standing "cover" behaviour: fill the tile, crop the overflow -
    // fine for portrait photos. True = "contain": scale the whole image to fit
    // with padding, so nothing is cropped - meant for flags and full-frame
    // logos where the edges carry meaning. Grid-level rather than per-entry
    // since a grid like "name that country" is entirely one or the other.
    // columnDefinition gives an explicit DB default, same reasoning as
    // entireCategoryPool above, so ddl-auto=update adds it cleanly.
    @Column(name = "fit_images", nullable = false, columnDefinition = "boolean default false")
    private boolean fitImages = false;

    public boolean isFitImages() {
        return fitImages;
    }

    public void setFitImages(boolean fitImages) {
        this.fitImages = fitImages;
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

    public boolean isRanked() {
        return ranked;
    }

    public void setRanked(boolean ranked) {
        this.ranked = ranked;
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

    // Pools this grid has imported candidates from, recorded automatically the
    // moment an admin uses "import from pool" in the grid editor. When a new
    // member is later added to any of these pools, that member is automatically
    // added as a candidate here too - this is what makes that propagation
    // possible, by knowing which grids to reach.
    @ManyToMany
    @JoinTable(
            name = "grid_pool_links",
            joinColumns = @JoinColumn(name = "grid_id"),
            inverseJoinColumns = @JoinColumn(name = "pool_id"))
    private Set<AthletePool> linkedPools = new HashSet<>();

    public Set<AthletePool> getLinkedPools() {
        return linkedPools;
    }

    public void setLinkedPools(Set<AthletePool> linkedPools) {
        // Same mutate-in-place reasoning as candidates/entries above - avoids
        // Hibernate deleting and re-inserting every join-table row on every save.
        Set<AthletePool> incoming = linkedPools != null ? linkedPools : new HashSet<>();
        this.linkedPools.removeIf(existing -> !incoming.contains(existing));
        for (AthletePool p : incoming) {
            if (!this.linkedPools.contains(p)) {
                this.linkedPools.add(p);
            }
        }
    }

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
