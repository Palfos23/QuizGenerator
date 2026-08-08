package com.quizapp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "grid_entries")
public class GridEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grid_id", nullable = false)
    @JsonIgnore
    private Grid grid;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "athlete_id", nullable = false)
    private Athlete athlete;

    // Short label shown on the tile, e.g. "FW", "WR", "GC" (general classification) -
    // admin's choice, and optional - some hints are too easy to guess with a label attached.
    @Column(name = "hint_label")
    private String hintLabel;

    // The number shown alongside the label, e.g. goals scored, races won.
    // Null for entries in an unranked grid (see Grid.ranked) - there's
    // nothing to show or sort by in that mode, the image itself is the hint.
    @Column(name = "hint_value")
    private Integer hintValue;

    // Display order for the tiles - the reference game sorts by hint value descending.
    @Column(name = "order_index", nullable = false)
    private int orderIndex;

    // Optional - which club's crest to show as an extra hint on this entry's tile.
    // Nullable on purpose: not every grid needs logos, and a club-themed grid vs. a
    // cross-club grid ("all-time PL scorers") pick a different club per entry anyway.
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "club_id")
    private Club club;

    // Nullable Boolean (not a primitive) so this column can be added to an existing,
    // non-empty grid_entries table without a NOT NULL/default migration headache -
    // null is treated as "show the logo" (the default) wherever this is read.
    @Column(name = "show_logo")
    private Boolean showLogo;

    // When true, this entry's hint-badge image comes directly from its own
    // athlete's photoUrl instead of a Club - for content where every entry's
    // image is genuinely unique to itself (e.g. movie posters), rather than
    // shared across many entries the way a sports team's crest is. Mutually
    // exclusive with club in practice, though not enforced at the database
    // level - the read-side logic simply checks this first.
    @Column(name = "use_own_photo_as_logo", nullable = false)
    private boolean useOwnPhotoAsLogo = false;

    public Club getClub() {
        return club;
    }

    public void setClub(Club club) {
        this.club = club;
    }

    public boolean isShowLogo() {
        return showLogo == null || showLogo;
    }

    public void setShowLogo(Boolean showLogo) {
        this.showLogo = showLogo;
    }

    public boolean isUseOwnPhotoAsLogo() {
        return useOwnPhotoAsLogo;
    }

    public void setUseOwnPhotoAsLogo(boolean useOwnPhotoAsLogo) {
        this.useOwnPhotoAsLogo = useOwnPhotoAsLogo;
    }

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

    public Athlete getAthlete() {
        return athlete;
    }

    public void setAthlete(Athlete athlete) {
        this.athlete = athlete;
    }

    public String getHintLabel() {
        return hintLabel;
    }

    public void setHintLabel(String hintLabel) {
        this.hintLabel = hintLabel;
    }

    public Integer getHintValue() {
        return hintValue;
    }

    public void setHintValue(Integer hintValue) {
        this.hintValue = hintValue;
    }

    public int getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(int orderIndex) {
        this.orderIndex = orderIndex;
    }
}
