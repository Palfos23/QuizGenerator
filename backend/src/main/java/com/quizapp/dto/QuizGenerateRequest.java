package com.quizapp.dto;

import com.quizapp.model.Language;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class QuizGenerateRequest {

    private String title;

    @NotNull
    private Language language;

    // Both null = any difficulty. Otherwise picks questions with difficultyLevel in [min, max].
    @Min(1) @Max(10)
    private Integer minDifficulty;

    @Min(1) @Max(10)
    private Integer maxDifficulty;

    // e.g. [{category: "Sport", numberOfQuestions: 3}, {category: "Film", numberOfQuestions: 4}]
    @NotEmpty(message = "Pick at least one category and how many questions you want from it")
    @Valid
    private List<CategorySelectionDto> categorySelections;

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

    public Integer getMinDifficulty() {
        return minDifficulty;
    }

    public void setMinDifficulty(Integer minDifficulty) {
        this.minDifficulty = minDifficulty;
    }

    public Integer getMaxDifficulty() {
        return maxDifficulty;
    }

    public void setMaxDifficulty(Integer maxDifficulty) {
        this.maxDifficulty = maxDifficulty;
    }

    public List<CategorySelectionDto> getCategorySelections() {
        return categorySelections;
    }

    public void setCategorySelections(List<CategorySelectionDto> categorySelections) {
        this.categorySelections = categorySelections;
    }
}
