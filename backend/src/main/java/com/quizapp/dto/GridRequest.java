package com.quizapp.dto;

import com.quizapp.model.Sport;
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
    private Sport sport;

    @NotNull
    private LocalDate weekStartDate;

    @NotNull
    private Integer maxStrikes;

    // The full searchable pool for this grid (includes both correct answers and decoys).
    @NotEmpty
    private List<Long> candidateAthleteIds;

    // The correct answers - each athleteId here must also appear in candidateAthleteIds.
    @NotEmpty
    @Valid
    private List<GridEntryInputDto> entries;

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

    public List<Long> getCandidateAthleteIds() {
        return candidateAthleteIds;
    }

    public void setCandidateAthleteIds(List<Long> candidateAthleteIds) {
        this.candidateAthleteIds = candidateAthleteIds;
    }

    public List<GridEntryInputDto> getEntries() {
        return entries;
    }

    public void setEntries(List<GridEntryInputDto> entries) {
        this.entries = entries;
    }
}
