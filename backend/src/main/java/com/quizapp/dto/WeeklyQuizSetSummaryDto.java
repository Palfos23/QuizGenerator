package com.quizapp.dto;

import java.time.LocalDate;

public class WeeklyQuizSetSummaryDto {

    private Long id;
    private LocalDate weekStartDate;
    private int questionCount;
    private String status; // NOT_STARTED / IN_PROGRESS / SUBMITTED / GRADED
    private Integer score; // only set once status == GRADED

    public WeeklyQuizSetSummaryDto(Long id, LocalDate weekStartDate, int questionCount, String status, Integer score) {
        this.id = id;
        this.weekStartDate = weekStartDate;
        this.questionCount = questionCount;
        this.status = status;
        this.score = score;
    }

    public Long getId() {
        return id;
    }

    public LocalDate getWeekStartDate() {
        return weekStartDate;
    }

    public int getQuestionCount() {
        return questionCount;
    }

    public String getStatus() {
        return status;
    }

    public Integer getScore() {
        return score;
    }
}
