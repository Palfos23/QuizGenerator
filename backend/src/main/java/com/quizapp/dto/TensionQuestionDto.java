package com.quizapp.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.time.Instant;
import java.util.List;

public class TensionQuestionDto {

    private Long id;

    @NotBlank
    private String title;

    private Instant updatedAt;

    private String mainCategory;

    private String answersCategory;
    private String source;

    @NotEmpty
    @Valid
    private List<TensionAnswerEntryDto> safeAnswers;

    @Valid
    private List<TensionAnswerEntryDto> tensionAnswers;

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

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getMainCategory() {
        return mainCategory;
    }

    public void setMainCategory(String mainCategory) {
        this.mainCategory = mainCategory;
    }

    public String getAnswersCategory() {
        return answersCategory;
    }

    public void setAnswersCategory(String answersCategory) {
        this.answersCategory = answersCategory;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public List<TensionAnswerEntryDto> getSafeAnswers() {
        return safeAnswers;
    }

    public void setSafeAnswers(List<TensionAnswerEntryDto> safeAnswers) {
        this.safeAnswers = safeAnswers;
    }

    public List<TensionAnswerEntryDto> getTensionAnswers() {
        return tensionAnswers;
    }

    public void setTensionAnswers(List<TensionAnswerEntryDto> tensionAnswers) {
        this.tensionAnswers = tensionAnswers;
    }
}
