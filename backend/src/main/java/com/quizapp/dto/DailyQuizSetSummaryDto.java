package com.quizapp.dto;

import java.time.LocalDate;

public class DailyQuizSetSummaryDto {

    private Long id;
    private LocalDate quizDate;
    private int questionCount;
    private String status; // NOT_STARTED / IN_PROGRESS / SUBMITTED / GRADED
    private Integer score; // only set once status == GRADED

    public DailyQuizSetSummaryDto(Long id, LocalDate quizDate, int questionCount, String status, Integer score) {
        this.id = id;
        this.quizDate = quizDate;
        this.questionCount = questionCount;
        this.status = status;
        this.score = score;
    }

    public Long getId() {
        return id;
    }

    public LocalDate getQuizDate() {
        return quizDate;
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
