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
        NAME_AND_LOGO, NAME_AND_PHOTO, PHOTO_ONLY, NAME_ONLY,
        // Name only until a tile is flipped, then its photo (and name) appear -
        // e.g. "Countries with green in their flag": guess from the name alone,
        // then the flag reveals. Unlike the other modes, this one's appearance
        // actually changes on flip; ImposterGridPlayService/ImposterOnlineService
        // already return a photo URL on every flip regardless of display mode,
        // so no backend change was needed beyond this constant - the frontend
        // just withholds the tile image until flippedTiles[t.id] is set.
        NAME_UNTIL_REVEALED
    }

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DisplayMode displayMode = DisplayMode.NAME_AND_PHOTO;

    // How a tile's image is fitted to the square tile. false (default) = the
    // long-standing "cover" behaviour: fill the tile, crop the overflow - fine
    // for portrait photos. true = "contain": scale the whole image to fit with
    // padding, nothing cropped - for flags and full-frame logos. Mirrors
    // Grid.fitImages; columnDefinition gives an explicit DB default so
    // ddl-auto=update adds it cleanly to an existing table.
    @Column(name = "fit_images", nullable = false, columnDefinition = "boolean default false")
    private boolean fitImages = false;

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

    public boolean isFitImages() {
        return fitImages;
    }

    public void setFitImages(boolean fitImages) {
        this.fitImages = fitImages;
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
