package com.quizapp.dto;

import java.time.LocalDate;

// "Your daily quiz has been reviewed" - one per result the player hasn't looked at yet.
public class DailyQuizReviewNotificationDto {

    private final Long setId;
    private final LocalDate quizDate;
    private final int score;
    private final int maxScore;

    public DailyQuizReviewNotificationDto(Long setId, LocalDate quizDate, int score, int maxScore) {
        this.setId = setId;
        this.quizDate = quizDate;
        this.score = score;
        this.maxScore = maxScore;
    }

    public Long getSetId() { return setId; }
    public LocalDate getQuizDate() { return quizDate; }
    public int getScore() { return score; }
    public int getMaxScore() { return maxScore; }
}
