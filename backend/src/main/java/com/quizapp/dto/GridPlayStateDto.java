package com.quizapp.dto;

import com.quizapp.model.Sport;

import java.util.List;

public class GridPlayStateDto {

    private Long id;
    private String title;
    private String theme;
    private Sport sport;
    private int maxStrikes;
    private int strikesUsed;
    private boolean completed;
    private boolean overtime;
    private boolean revealed;
    private List<GridEntryViewDto> entries;

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

    public int getMaxStrikes() {
        return maxStrikes;
    }

    public void setMaxStrikes(int maxStrikes) {
        this.maxStrikes = maxStrikes;
    }

    public int getStrikesUsed() {
        return strikesUsed;
    }

    public void setStrikesUsed(int strikesUsed) {
        this.strikesUsed = strikesUsed;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public boolean isOvertime() {
        return overtime;
    }

    public void setOvertime(boolean overtime) {
        this.overtime = overtime;
    }

    public boolean isRevealed() {
        return revealed;
    }

    public void setRevealed(boolean revealed) {
        this.revealed = revealed;
    }

    public List<GridEntryViewDto> getEntries() {
        return entries;
    }

    public void setEntries(List<GridEntryViewDto> entries) {
        this.entries = entries;
    }
}
