package com.quizapp.dto;

import jakarta.validation.constraints.NotBlank;

public class BullseyeOnlineAnswerRequest {
    @NotBlank
    private String guessedName;

    public String getGuessedName() { return guessedName; }
    public void setGuessedName(String guessedName) { this.guessedName = guessedName; }
}
