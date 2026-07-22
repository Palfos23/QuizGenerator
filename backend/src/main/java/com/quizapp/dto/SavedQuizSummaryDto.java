package com.quizapp.dto;

import com.quizapp.model.Language;

import java.time.Instant;

public class SavedQuizSummaryDto {

    private Long id;
    private String title;
    private Language language;
    private int questionCount;
    private Instant createdAt;

    public SavedQuizSummaryDto(Long id, String title, Language language, int questionCount, Instant createdAt) {
        this.id = id;
        this.title = title;
        this.language = language;
        this.questionCount = questionCount;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public Language getLanguage() {
        return language;
    }

    public int getQuestionCount() {
        return questionCount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
