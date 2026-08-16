package com.quizapp.dto;

import java.time.LocalDate;

public class LineupSummaryDto {

    private Long id;
    private String title;
    private String competition;
    private String teamName;
    private String opponentName;
    private Integer scoreFor;
    private Integer scoreAgainst;
    private LocalDate matchDate;
    private LocalDate weekStartDate;
    private String formation;
    private Integer maxStrikes;
    private boolean excludedFromBattle;

    public LineupSummaryDto(Long id, String title, String competition, String teamName, String opponentName,
                             Integer scoreFor, Integer scoreAgainst, LocalDate matchDate, LocalDate weekStartDate,
                             String formation) {
        this.id = id;
        this.title = title;
        this.competition = competition;
        this.teamName = teamName;
        this.opponentName = opponentName;
        this.scoreFor = scoreFor;
        this.scoreAgainst = scoreAgainst;
        this.matchDate = matchDate;
        this.weekStartDate = weekStartDate;
        this.formation = formation;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getCompetition() { return competition; }
    public String getTeamName() { return teamName; }
    public String getOpponentName() { return opponentName; }
    public Integer getScoreFor() { return scoreFor; }
    public Integer getScoreAgainst() { return scoreAgainst; }
    public LocalDate getMatchDate() { return matchDate; }
    public LocalDate getWeekStartDate() { return weekStartDate; }
    public String getFormation() { return formation; }
    public Integer getMaxStrikes() { return maxStrikes; }
    public void setMaxStrikes(Integer maxStrikes) { this.maxStrikes = maxStrikes; }
    public boolean isExcludedFromBattle() { return excludedFromBattle; }
    public void setExcludedFromBattle(boolean excludedFromBattle) { this.excludedFromBattle = excludedFromBattle; }
}
