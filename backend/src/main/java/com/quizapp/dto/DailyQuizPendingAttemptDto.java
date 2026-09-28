package com.quizapp.dto;

import java.time.LocalDate;

// One row per player+day that still has at least one non-exact answer
// waiting on a human decision - see DailyQuizReviewService.listPendingAttempts.
public class DailyQuizPendingAttemptDto {

    private Long attemptId;
    private String playerName;
    private LocalDate quizDate;
    private int pendingCount;

    public DailyQuizPendingAttemptDto(Long attemptId, String playerName, LocalDate quizDate, int pendingCount) {
        this.attemptId = attemptId;
        this.playerName = playerName;
        this.quizDate = quizDate;
        this.pendingCount = pendingCount;
    }

    public Long getAttemptId() {
        return attemptId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public LocalDate getQuizDate() {
        return quizDate;
    }

    public int getPendingCount() {
        return pendingCount;
    }
}
