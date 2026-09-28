package com.quizapp.dto;

import jakarta.validation.constraints.NotNull;

public class WeeklyQuizResolveRequest {

    @NotNull
    private Boolean correct;

    public Boolean getCorrect() {
        return correct;
    }

    public void setCorrect(Boolean correct) {
        this.correct = correct;
    }
}
