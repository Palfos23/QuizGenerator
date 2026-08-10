package com.quizapp.dto;

import java.util.List;

public class ImposterPlayStateDto {
    private Long id;
    private String title;
    private String description;
    private String displayMode;
    private List<TileView> tiles;

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

    public String getDisplayMode() {
        return displayMode;
    }

    public void setDisplayMode(String displayMode) {
        this.displayMode = displayMode;
    }

    public List<TileView> getTiles() {
        return tiles;
    }

    public void setTiles(List<TileView> tiles) {
        this.tiles = tiles;
    }

    public static class TileView {
        private Long id;
        private String athleteName;
        private String photoUrl;
        private String logoUrl;

        public TileView(Long id, String athleteName, String photoUrl, String logoUrl) {
            this.id = id;
            this.athleteName = athleteName;
            this.photoUrl = photoUrl;
            this.logoUrl = logoUrl;
        }

        public Long getId() {
            return id;
        }

        public String getAthleteName() {
            return athleteName;
        }

        public String getPhotoUrl() {
            return photoUrl;
        }

        public String getLogoUrl() {
            return logoUrl;
        }
    }
}
