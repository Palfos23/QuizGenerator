package com.quizapp.dto;

import java.time.LocalDate;

public class PenaltyShootoutSummaryDto {

    private Long id;
    private String title;
    private String competition;
    private String teamName;
    private String opponentName;
    private String teamCrestUrl;
    private String opponentCrestUrl;
    private Integer teamPensScored;
    private Integer opponentPensScored;
    private LocalDate matchDate;
    private int maxStrikes;
    private int kickCount;

    public PenaltyShootoutSummaryDto(Long id, String title, String competition, String teamName, String opponentName,
                                      String teamCrestUrl, String opponentCrestUrl, Integer teamPensScored,
                                      Integer opponentPensScored, LocalDate matchDate, int maxStrikes, int kickCount) {
        this.id = id;
        this.title = title;
        this.competition = competition;
        this.teamName = teamName;
        this.opponentName = opponentName;
        this.teamCrestUrl = teamCrestUrl;
        this.opponentCrestUrl = opponentCrestUrl;
        this.teamPensScored = teamPensScored;
        this.opponentPensScored = opponentPensScored;
        this.matchDate = matchDate;
        this.maxStrikes = maxStrikes;
        this.kickCount = kickCount;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getCompetition() { return competition; }
    public String getTeamName() { return teamName; }
    public String getOpponentName() { return opponentName; }
    public String getTeamCrestUrl() { return teamCrestUrl; }
    public String getOpponentCrestUrl() { return opponentCrestUrl; }
    public Integer getTeamPensScored() { return teamPensScored; }
    public Integer getOpponentPensScored() { return opponentPensScored; }
    public LocalDate getMatchDate() { return matchDate; }
    public int getMaxStrikes() { return maxStrikes; }
    public int getKickCount() { return kickCount; }
}
