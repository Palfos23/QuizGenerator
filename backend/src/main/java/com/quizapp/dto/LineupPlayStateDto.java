package com.quizapp.dto;

import java.time.LocalDate;
import java.util.List;

// Starting state for solo (or local pass-and-play) Starting XI - stateless,
// same reasoning as GridPlayStateDto's multiplayer-start counterpart: no
// persisted attempt, the client tracks its own guessed/strikes state and
// calls /guess and /reveal as needed.
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
    private int maxStrikes;
    private List<LineupSlotDto> slots;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getCompetition() { return competition; }
    public void setCompetition(String competition) { this.competition = competition; }
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
    public LocalDate getMatchDate() { return matchDate; }
    public void setMatchDate(LocalDate matchDate) { this.matchDate = matchDate; }
    public String getFormation() { return formation; }
    public void setFormation(String formation) { this.formation = formation; }
    public int getMaxStrikes() { return maxStrikes; }
    public void setMaxStrikes(int maxStrikes) { this.maxStrikes = maxStrikes; }
    public List<LineupSlotDto> getSlots() { return slots; }
    public void setSlots(List<LineupSlotDto> slots) { this.slots = slots; }
}
