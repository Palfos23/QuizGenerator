package com.quizapp.dto;

import jakarta.validation.constraints.NotBlank;

public class TensionOnlineAnswerRequest {
    @NotBlank
    private String answerText;

    public String getAnswerText() { return answerText; }
    public void setAnswerText(String answerText) { this.answerText = answerText; }
}
