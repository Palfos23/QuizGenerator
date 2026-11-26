package com.quizapp.dto;

import java.time.LocalDate;
import java.util.List;

// Admin-facing: everyone who has submitted one day's daily quiz - including players whose answers
// have all been decided already, who no longer appear in the "waiting for review" list. This is how
// an admin gets back to an attempt to correct a decision they got wrong.
public class DailyQuizDayAttemptsDto {

    private LocalDate quizDate;
    private List<AttemptRow> attempts; // just the requested page
    private int page; // 0-based
    private int size;
    private int matching; // rows matching the search and filter, across all pages
    private int total; // everyone who submitted this day
    private int pendingTotal; // of those, still waiting on a decision
    private int gradedTotal;

    public DailyQuizDayAttemptsDto(LocalDate quizDate, List<AttemptRow> attempts, int page, int size,
                                   int matching, int total, int pendingTotal, int gradedTotal) {
        this.quizDate = quizDate;
        this.attempts = attempts;
        this.page = page;
        this.size = size;
        this.matching = matching;
        this.total = total;
        this.pendingTotal = pendingTotal;
        this.gradedTotal = gradedTotal;
    }

    public int getPage() { return page; }
    public int getSize() { return size; }
    public int getMatching() { return matching; }
    public int getTotal() { return total; }
    public int getPendingTotal() { return pendingTotal; }
    public int getGradedTotal() { return gradedTotal; }

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
