package com.quizapp.dto;

public class ImposterGridSummaryDto {
    private Long id;
    private String title;
    private String description;
    private String sport;
    private int tileCount;
    private int imposterCount; // fair-game info, doesn't reveal which tiles

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

    public int getTileCount() {
        return tileCount;
    }

    public void setTileCount(int tileCount) {
        this.tileCount = tileCount;
    }

    public int getImposterCount() {
        return imposterCount;
    }

    public void setImposterCount(int imposterCount) {
        this.imposterCount = imposterCount;
    }
}
