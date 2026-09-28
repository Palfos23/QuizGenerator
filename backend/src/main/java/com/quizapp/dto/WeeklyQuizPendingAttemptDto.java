package com.quizapp.dto;

import java.time.LocalDate;

// One row per player+week that still has at least one non-exact answer
// waiting on a human decision - see WeeklyQuizReviewService.listPendingAttempts.
public class WeeklyQuizPendingAttemptDto {

    private Long attemptId;
    private String playerName;
    private LocalDate weekStartDate;
    private int pendingCount;

    public WeeklyQuizPendingAttemptDto(Long attemptId, String playerName, LocalDate weekStartDate, int pendingCount) {
        this.attemptId = attemptId;
        this.playerName = playerName;
        this.weekStartDate = weekStartDate;
        this.pendingCount = pendingCount;
    }

    public Long getAttemptId() {
        return attemptId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public LocalDate getWeekStartDate() {
        return weekStartDate;
    }

    public int getPendingCount() {
        return pendingCount;
    }
}
