package com.quizapp.dto;

import java.util.List;

public class ImposterGridAdminDetailDto {
    private Long id;
    private String title;
    private String description;
    private String sport;
    private String displayMode;
    private List<TileDetail> tiles;

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

    public String getDisplayMode() {
        return displayMode;
    }

    public void setDisplayMode(String displayMode) {
        this.displayMode = displayMode;
    }

    public List<TileDetail> getTiles() {
        return tiles;
    }

    public void setTiles(List<TileDetail> tiles) {
        this.tiles = tiles;
    }

    public static class TileDetail {
        private Long id;
        private AthleteDto athlete;
        private boolean imposter;
        private AthleteDto replacedAthlete;
        private ClubDto club;

        public TileDetail(Long id, AthleteDto athlete, boolean imposter, AthleteDto replacedAthlete, ClubDto club) {
            this.id = id;
            this.athlete = athlete;
            this.imposter = imposter;
            this.replacedAthlete = replacedAthlete;
            this.club = club;
        }

        public ClubDto getClub() {
            return club;
        }

        public Long getId() {
            return id;
        }

        public AthleteDto getAthlete() {
            return athlete;
        }

        public boolean isImposter() {
            return imposter;
        }

        public AthleteDto getReplacedAthlete() {
            return replacedAthlete;
        }
    }
}
