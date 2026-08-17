package com.quizapp.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public class LineupRequest {

    @NotBlank
    private String title;

    private String competition;

    private LocalDate matchDate;

    @NotNull
    private LocalDate weekStartDate;

    @NotBlank
    private String formation;

    @NotBlank
    private String teamName;

    private String teamCrestUrl;

    @NotBlank
    private String opponentName;

    private String opponentCrestUrl;

    private Integer scoreFor;
    private Integer scoreAgainst;

    @NotNull
    private Integer maxStrikes;

    private boolean excludedFromBattle = false;

    private String kitColor;
    private String goalkeeperKitColor;

    // Full searchable pool (correct starters + decoys).
    @NotEmpty
    private List<Long> candidateAthleteIds;

    // Exactly 11 entries, one per formation slot.
    @NotEmpty
    @Valid
    private List<LineupEntryInputDto> entries;

    // Pools imported from during this edit - merged into the board's
    // linkedPools so future additions to the pool propagate automatically.
    // Same convention as GridRequest.linkedPoolIds.
    private List<Long> linkedPoolIds;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getCompetition() { return competition; }
    public void setCompetition(String competition) { this.competition = competition; }
    public LocalDate getMatchDate() { return matchDate; }
    public void setMatchDate(LocalDate matchDate) { this.matchDate = matchDate; }
    public LocalDate getWeekStartDate() { return weekStartDate; }
    public void setWeekStartDate(LocalDate weekStartDate) { this.weekStartDate = weekStartDate; }
    public String getFormation() { return formation; }
    public void setFormation(String formation) { this.formation = formation; }
    public String getTeamName() { return teamName; }
    public void setTeamName(String teamName) { this.teamName = teamName; }
    public String getTeamCrestUrl() { return teamCrestUrl; }
    public void setTeamCrestUrl(String teamCrestUrl) { this.teamCrestUrl = teamCrestUrl; }
    public String getOpponentName() { return opponentName; }
    public void setOpponentName(String opponentName) { this.opponentName = opponentName; }
    public String getOpponentCrestUrl() { return opponentCrestUrl; }
    public void setOpponentCrestUrl(String opponentCrestUrl) { this.opponentCrestUrl = opponentCrestUrl; }
    public Integer getScoreFor() { return scoreFor; }
    public void setScoreFor(Integer scoreFor) { this.scoreFor = scoreFor; }
    public Integer getScoreAgainst() { return scoreAgainst; }
    public void setScoreAgainst(Integer scoreAgainst) { this.scoreAgainst = scoreAgainst; }
    public Integer getMaxStrikes() { return maxStrikes; }
    public void setMaxStrikes(Integer maxStrikes) { this.maxStrikes = maxStrikes; }
    public boolean isExcludedFromBattle() { return excludedFromBattle; }
    public void setExcludedFromBattle(boolean excludedFromBattle) { this.excludedFromBattle = excludedFromBattle; }
    public String getKitColor() { return kitColor; }
    public void setKitColor(String kitColor) { this.kitColor = kitColor; }
    public String getGoalkeeperKitColor() { return goalkeeperKitColor; }
    public void setGoalkeeperKitColor(String goalkeeperKitColor) { this.goalkeeperKitColor = goalkeeperKitColor; }
    public List<Long> getCandidateAthleteIds() { return candidateAthleteIds; }
    public void setCandidateAthleteIds(List<Long> candidateAthleteIds) { this.candidateAthleteIds = candidateAthleteIds; }
    public List<LineupEntryInputDto> getEntries() { return entries; }
    public void setEntries(List<LineupEntryInputDto> entries) { this.entries = entries; }
    public List<Long> getLinkedPoolIds() { return linkedPoolIds; }
    public void setLinkedPoolIds(List<Long> linkedPoolIds) { this.linkedPoolIds = linkedPoolIds; }
}
