package com.quizapp.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public class PenaltyShootoutRequest {

    @NotBlank
    private String title;

    private String competition;

    private LocalDate matchDate;

    @NotBlank
    private String teamName;

    private String teamCrestUrl;

    @NotBlank
    private String opponentName;

    private String opponentCrestUrl;

    private Integer teamPensScored;
    private Integer opponentPensScored;

    @NotNull
    private Integer maxStrikes;

    // Full searchable pool (kickers + decoys).
    @NotEmpty
    private List<Long> candidateAthleteIds;

    // The kicks themselves, in real shootout order.
    @NotEmpty
    @Valid
    private List<PenaltyKickInputDto> kicks;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getCompetition() { return competition; }
    public void setCompetition(String competition) { this.competition = competition; }
    public LocalDate getMatchDate() { return matchDate; }
    public void setMatchDate(LocalDate matchDate) { this.matchDate = matchDate; }
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
    public Integer getMaxStrikes() { return maxStrikes; }
    public void setMaxStrikes(Integer maxStrikes) { this.maxStrikes = maxStrikes; }
    public List<Long> getCandidateAthleteIds() { return candidateAthleteIds; }
    public void setCandidateAthleteIds(List<Long> candidateAthleteIds) { this.candidateAthleteIds = candidateAthleteIds; }
    public List<PenaltyKickInputDto> getKicks() { return kicks; }
    public void setKicks(List<PenaltyKickInputDto> kicks) { this.kicks = kicks; }
}
