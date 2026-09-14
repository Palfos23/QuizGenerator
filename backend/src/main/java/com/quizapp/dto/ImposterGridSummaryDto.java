package com.quizapp.dto;

import java.time.Instant;

public class ImposterGridSummaryDto {
    private Long id;
    private String title;
    private String description;
    private String sport;
    private int tileCount;
    private int imposterCount; // fair-game info, doesn't reveal which tiles
    private boolean canExpire;
    private Instant updatedAt;

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

    public boolean isCanExpire() {
        return canExpire;
    }

    public void setCanExpire(boolean canExpire) {
        this.canExpire = canExpire;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
