package com.quizapp.dto;

import com.quizapp.model.Language;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.List;

public class QuizDto {

    private Long id; // set only when this quiz has been saved ("My Quizzes")

    private String title;

    @NotNull
    private Language language;

    @NotEmpty
    @Valid
    private List<QuestionDto> questions;

    // e.g. "Only found 2 of the 4 requested Film questions" - empty when everything matched exactly
    private List<String> warnings = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Language getLanguage() {
        return language;
    }

    public void setLanguage(Language language) {
        this.language = language;
    }

    public List<QuestionDto> getQuestions() {
        return questions;
    }

    public void setQuestions(List<QuestionDto> questions) {
        this.questions = questions;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }
}
