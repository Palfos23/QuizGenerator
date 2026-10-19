package com.quizapp.dto;

import jakarta.validation.constraints.NotNull;

public class BdayScoreRequest {
    @NotNull
    private Integer score;

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }
}
