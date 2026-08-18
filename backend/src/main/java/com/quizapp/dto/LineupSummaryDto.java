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

    // Only set on the admin management list - same purpose as
    // GridSummaryDto.entireCategoryPool.
    private boolean entireCategoryPool;
    public boolean isEntireCategoryPool() { return entireCategoryPool; }
    public void setEntireCategoryPool(boolean entireCategoryPool) { this.entireCategoryPool = entireCategoryPool; }

    // Only meaningful on the user-facing list - null on the admin management
    // list, where "your own progress" isn't a relevant concept. Same purpose
    // as GridSummaryDto's status/guessedCount pair.
    private String status; // "NOT_STARTED" | "IN_PROGRESS" | "COMPLETED"
    private Integer guessedCount;
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getGuessedCount() { return guessedCount; }
    public void setGuessedCount(Integer guessedCount) { this.guessedCount = guessedCount; }

    // Always 11 for a valid published board, but carried explicitly (like
    // GridSummaryDto.entryCount) rather than hardcoded on the frontend.
    private int entryCount;
    public int getEntryCount() { return entryCount; }
    public void setEntryCount(int entryCount) { this.entryCount = entryCount; }
}
