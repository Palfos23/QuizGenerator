package com.quizapp.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

// "Imposter" game mode: a set of visible tiles (name/photo already shown, not
// hidden like Grid) where most subjects genuinely fit a theme and a few are
// deliberate plants that don't. Players flip tiles one at a time trying to
// avoid the imposters - lowest imposter-hit count wins.
@Entity
@Table(name = "imposter_grids")
public class ImposterGrid {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String title;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private String sport;

    // How each tile displays its subject - grid-wide, not per-tile, since
    // the whole point is a consistent set of tiles to compare against each
    // other.
    public enum DisplayMode {
        NAME_AND_LOGO, NAME_AND_PHOTO, PHOTO_ONLY, NAME_ONLY
    }

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DisplayMode displayMode = DisplayMode.NAME_AND_PHOTO;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @OneToMany(mappedBy = "imposterGrid", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<ImposterTile> tiles = new ArrayList<>();

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

    public String getSport() {
        return sport;
    }

    public void setSport(String sport) {
        this.sport = sport;
    }

    public DisplayMode getDisplayMode() {
        return displayMode;
    }

    public void setDisplayMode(DisplayMode displayMode) {
        this.displayMode = displayMode;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public List<ImposterTile> getTiles() {
        return tiles;
    }

    public void setTiles(List<ImposterTile> tiles) {
        this.tiles.clear();
        if (tiles != null) {
            for (ImposterTile t : tiles) {
                t.setImposterGrid(this);
                this.tiles.add(t);
            }
        }
    }
}
