package com.quizapp.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

// Play state for a Penalty Shootout board. Always the stateless-start shape -
// unlike LineupPlayStateDto there's no persisted-attempt variant here, since
// this game has no weekly solo mode (see PenaltyShootout's class comment) -
// solo and pass-and-play both just start from a blank board and track
// progress client-side, the same way Lineup's *multiplayer*-start path
// already does.
public class PenaltyShootoutPlayStateDto {

    private Long id;
    private String title;
    private String competition;
    private String teamName;
    private String teamCrestUrl;
    private String opponentName;
    private String opponentCrestUrl;
    private Integer teamPensScored;
    private Integer opponentPensScored;
    private LocalDate matchDate;
    private Instant updatedAt;
    private int maxStrikes;
    private List<PenaltyKickDto> kicks;

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
    public Integer getTeamPensScored() { return teamPensScored; }
    public void setTeamPensScored(Integer teamPensScored) { this.teamPensScored = teamPensScored; }
    public Integer getOpponentPensScored() { return opponentPensScored; }
    public void setOpponentPensScored(Integer opponentPensScored) { this.opponentPensScored = opponentPensScored; }
    public LocalDate getMatchDate() { return matchDate; }
    public void setMatchDate(LocalDate matchDate) { this.matchDate = matchDate; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public int getMaxStrikes() { return maxStrikes; }
    public void setMaxStrikes(int maxStrikes) { this.maxStrikes = maxStrikes; }
    public List<PenaltyKickDto> getKicks() { return kicks; }
    public void setKicks(List<PenaltyKickDto> kicks) { this.kicks = kicks; }
}
