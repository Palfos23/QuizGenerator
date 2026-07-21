package com.quizapp.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class EmailQuizRequest {

    @NotBlank
    @Email
    private String recipientEmail;

    private boolean includeAnswers = true;

    @NotNull
    @Valid
    private QuizDto quiz;

    public String getRecipientEmail() {
        return recipientEmail;
    }

    public void setRecipientEmail(String recipientEmail) {
        this.recipientEmail = recipientEmail;
    }

    public boolean isIncludeAnswers() {
        return includeAnswers;
    }

    public void setIncludeAnswers(boolean includeAnswers) {
        this.includeAnswers = includeAnswers;
    }

    public QuizDto getQuiz() {
        return quiz;
    }

    public void setQuiz(QuizDto quiz) {
        this.quiz = quiz;
    }
}
