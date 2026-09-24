package com.quizapp.dto;

import java.time.Instant;
import java.util.List;

// Everything a self-service "download my data" request hands back - see
// AccountService#exportData. Grouped into nested classes in one file rather
// than a dozen tiny top-level DTOs, same call made for the birthday-quiz DTOs
// earlier in this project.
public class AccountExportDto {
    private String email;
    private String name;
    private boolean googleLinked;
    private Instant createdAt;
    private List<SavedQuizExport> savedQuizzes;
    private List<SubmittedQuestionExport> submittedQuestions;
    private List<ReportExport> reports;
    private List<GridAttemptExport> gridAttempts;
    private List<LineupAttemptExport> lineupAttempts;

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public boolean isGoogleLinked() { return googleLinked; }
    public void setGoogleLinked(boolean googleLinked) { this.googleLinked = googleLinked; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public List<SavedQuizExport> getSavedQuizzes() { return savedQuizzes; }
    public void setSavedQuizzes(List<SavedQuizExport> savedQuizzes) { this.savedQuizzes = savedQuizzes; }
    public List<SubmittedQuestionExport> getSubmittedQuestions() { return submittedQuestions; }
    public void setSubmittedQuestions(List<SubmittedQuestionExport> submittedQuestions) { this.submittedQuestions = submittedQuestions; }
    public List<ReportExport> getReports() { return reports; }
    public void setReports(List<ReportExport> reports) { this.reports = reports; }
    public List<GridAttemptExport> getGridAttempts() { return gridAttempts; }
    public void setGridAttempts(List<GridAttemptExport> gridAttempts) { this.gridAttempts = gridAttempts; }
    public List<LineupAttemptExport> getLineupAttempts() { return lineupAttempts; }
    public void setLineupAttempts(List<LineupAttemptExport> lineupAttempts) { this.lineupAttempts = lineupAttempts; }

    public static class SavedQuizExport {
        private String title;
        private String language;
        private Instant createdAt;

        public SavedQuizExport(String title, String language, Instant createdAt) {
            this.title = title;
            this.language = language;
            this.createdAt = createdAt;
        }

        public String getTitle() { return title; }
        public String getLanguage() { return language; }
        public Instant getCreatedAt() { return createdAt; }
    }

    public static class SubmittedQuestionExport {
        private String questionText;
        private String answer;
        private String category;
        private String status;
        private Instant createdAt;

        public SubmittedQuestionExport(String questionText, String answer, String category, String status, Instant createdAt) {
            this.questionText = questionText;
            this.answer = answer;
            this.category = category;
            this.status = status;
            this.createdAt = createdAt;
        }

        public String getQuestionText() { return questionText; }
        public String getAnswer() { return answer; }
        public String getCategory() { return category; }
        public String getStatus() { return status; }
        public Instant getCreatedAt() { return createdAt; }
    }

    public static class ReportExport {
        private String area;
        private String message;
        private String status;
        private Instant createdAt;

        public ReportExport(String area, String message, String status, Instant createdAt) {
            this.area = area;
            this.message = message;
            this.status = status;
            this.createdAt = createdAt;
        }

        public String getArea() { return area; }
        public String getMessage() { return message; }
        public String getStatus() { return status; }
        public Instant getCreatedAt() { return createdAt; }
    }

    public static class GridAttemptExport {
        private String gridTitle;
        private boolean completed;
        private int strikesUsed;

        public GridAttemptExport(String gridTitle, boolean completed, int strikesUsed) {
            this.gridTitle = gridTitle;
            this.completed = completed;
            this.strikesUsed = strikesUsed;
        }

        public String getGridTitle() { return gridTitle; }
        public boolean isCompleted() { return completed; }
        public int getStrikesUsed() { return strikesUsed; }
    }

    public static class LineupAttemptExport {
        private String lineupTitle;
        private boolean completed;
        private int strikesUsed;

        public LineupAttemptExport(String lineupTitle, boolean completed, int strikesUsed) {
            this.lineupTitle = lineupTitle;
            this.completed = completed;
            this.strikesUsed = strikesUsed;
        }

        public String getLineupTitle() { return lineupTitle; }
        public boolean isCompleted() { return completed; }
        public int getStrikesUsed() { return strikesUsed; }
    }
}
