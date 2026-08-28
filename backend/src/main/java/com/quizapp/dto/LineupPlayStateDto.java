package com.quizapp.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

// Play state for a Starting XI board - shared shape for both the persisted
// solo/weekly path (strikesUsed/completed/revealed reflect a server-side
// LineupAttempt) and the stateless local pass-and-play multiplayer-start
// path (those three always start at their defaults, since there's no
// attempt to attach to - see LineupPlayService for the split, mirroring
// GridPlayStateDto's play vs. multiplayer-start distinction).
public class LineupPlayStateDto {

    private Long id;
    private String title;
    private String competition;
    private String teamName;
    private String teamCrestUrl;
    private String opponentName;
    private String opponentCrestUrl;
    private Integer scoreFor;
    private Integer scoreAgainst;
    private LocalDate matchDate;
    private String formation;
    private Instant updatedAt;
    private int maxStrikes;
    private int strikesUsed;
    private boolean completed;
    private boolean revealed;
    private String kitColor;
    private String goalkeeperKitColor;
    private List<LineupSlotDto> slots;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getCompetition() { return competition; }
    public void setCompetition(String competition) { this.competition = competition; }
    public String getTeamName() { return teamName; }
    public void setTeamName(String teamName) { this.teamName = teamName; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
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
    public LocalDate getMatchDate() { return matchDate; }
    public void setMatchDate(LocalDate matchDate) { this.matchDate = matchDate; }
    public String getFormation() { return formation; }
    public void setFormation(String formation) { this.formation = formation; }
    public int getMaxStrikes() { return maxStrikes; }
    public void setMaxStrikes(int maxStrikes) { this.maxStrikes = maxStrikes; }
    public int getStrikesUsed() { return strikesUsed; }
    public void setStrikesUsed(int strikesUsed) { this.strikesUsed = strikesUsed; }
    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }
    public boolean isRevealed() { return revealed; }
    public void setRevealed(boolean revealed) { this.revealed = revealed; }
    public String getKitColor() { return kitColor; }
    public void setKitColor(String kitColor) { this.kitColor = kitColor; }
    public String getGoalkeeperKitColor() { return goalkeeperKitColor; }
    public void setGoalkeeperKitColor(String goalkeeperKitColor) { this.goalkeeperKitColor = goalkeeperKitColor; }
    public List<LineupSlotDto> getSlots() { return slots; }
    public void setSlots(List<LineupSlotDto> slots) { this.slots = slots; }
}
