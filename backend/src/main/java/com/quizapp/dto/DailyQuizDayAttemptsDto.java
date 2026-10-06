package com.quizapp.dto;

import java.time.LocalDate;
import java.util.List;

// Admin-facing: everyone who has submitted one day's daily quiz - including players whose answers
// have all been decided already, who no longer appear in the "waiting for review" list. This is how
// an admin gets back to an attempt to correct a decision they got wrong.
public class DailyQuizDayAttemptsDto {

    private LocalDate quizDate;
    private List<AttemptRow> attempts;

    public DailyQuizDayAttemptsDto(LocalDate quizDate, List<AttemptRow> attempts) {
        this.quizDate = quizDate;
        this.attempts = attempts;
    }

    public LocalDate getQuizDate() {
        return quizDate;
    }

    public List<AttemptRow> getAttempts() {
        return attempts;
    }

    public static class AttemptRow {
        private Long attemptId;
        private String playerName;
        private String status; // SUBMITTED / GRADED
        private Integer score; // null until GRADED
        private int maxScore;
        private int pendingCount;

        public AttemptRow(Long attemptId, String playerName, String status, Integer score, int maxScore, int pendingCount) {
            this.attemptId = attemptId;
            this.playerName = playerName;
            this.status = status;
            this.score = score;
            this.maxScore = maxScore;
            this.pendingCount = pendingCount;
        }

        public Long getAttemptId() { return attemptId; }
        public String getPlayerName() { return playerName; }
        public String getStatus() { return status; }
        public Integer getScore() { return score; }
        public int getMaxScore() { return maxScore; }
        public int getPendingCount() { return pendingCount; }
    }
}
