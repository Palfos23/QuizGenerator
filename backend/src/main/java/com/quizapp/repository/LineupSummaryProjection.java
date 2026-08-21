package com.quizapp.repository;

import java.time.LocalDate;

public interface LineupSummaryProjection {
    Long getId();
    String getTitle();
    String getCompetition();
    String getTeamName();
    String getOpponentName();
    Integer getScoreFor();
    Integer getScoreAgainst();
    LocalDate getMatchDate();
    LocalDate getWeekStartDate();
    String getFormation();
    Long getEntryCount();
    Boolean getExcludedFromBattle();
}
