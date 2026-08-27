package com.quizapp.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public class GridRequest {

    @NotBlank
    private String title;

    private String theme;

    @NotNull
    private String sport;

    @NotNull
    private LocalDate weekStartDate;

    @NotNull
    private Integer maxStrikes;

    private boolean sortAscending = false;
    private boolean ranked = true;
    private boolean excludedFromGridBattle = false;
    private String revealMode = "PHOTO"; // "PHOTO" or "DESCRIPTION"
    private boolean fitImages = false; // true = contain (show whole image, e.g. flags); false = cover

    // When true, ignore candidateAthleteIds entirely - GridAdminService stores
    // no explicit candidates at all and the search box draws live from every
    // Athlete in "sport" instead. Only required (@NotEmpty enforced manually
    // in GridAdminService, not here) when this is false.
    private boolean entireCategoryPool = false;

    // The full searchable pool for this grid (includes both correct answers and decoys).
    // Not @NotEmpty here - required unless entireCategoryPool is true, which
    // GridAdminService validates since that depends on another field.
    private List<Long> candidateAthleteIds;

    // The correct answers - each athleteId here must also appear in candidateAthleteIds.
    @NotEmpty
    @Valid
    private List<GridEntryInputDto> entries;

    // Pools this grid has imported candidates from during this editing session -
    // recorded so a new member added to any of these pools later can be
    // automatically propagated here too. Optional - null/empty is fine for a
    // grid that was never built from a pool import.
    private List<Long> linkedPoolIds;

    public List<Long> getLinkedPoolIds() {
        return linkedPoolIds;
    }

    public void setLinkedPoolIds(List<Long> linkedPoolIds) {
        this.linkedPoolIds = linkedPoolIds;
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

    public Integer getMaxStrikes() {
        return maxStrikes;
    }

    public void setMaxStrikes(Integer maxStrikes) {
        this.maxStrikes = maxStrikes;
    }

    public boolean isSortAscending() {
        return sortAscending;
    }

    public boolean isRanked() {
        return ranked;
    }

    public void setRanked(boolean ranked) {
        this.ranked = ranked;
    }

    public void setSortAscending(boolean sortAscending) {
        this.sortAscending = sortAscending;
    }

    public boolean isExcludedFromGridBattle() {
        return excludedFromGridBattle;
    }

    public void setExcludedFromGridBattle(boolean excludedFromGridBattle) {
        this.excludedFromGridBattle = excludedFromGridBattle;
    }

    public String getRevealMode() {
        return revealMode;
    }

    public void setRevealMode(String revealMode) {
        this.revealMode = revealMode;
    }

    public boolean isFitImages() {
        return fitImages;
    }

    public void setFitImages(boolean fitImages) {
        this.fitImages = fitImages;
    }

    public List<Long> getCandidateAthleteIds() {
        return candidateAthleteIds;
    }

    public void setCandidateAthleteIds(List<Long> candidateAthleteIds) {
        this.candidateAthleteIds = candidateAthleteIds;
    }

    public boolean isEntireCategoryPool() {
        return entireCategoryPool;
    }

    public void setEntireCategoryPool(boolean entireCategoryPool) {
        this.entireCategoryPool = entireCategoryPool;
    }

    public List<GridEntryInputDto> getEntries() {
        return entries;
    }

    public void setEntries(List<GridEntryInputDto> entries) {
        this.entries = entries;
    }
}
