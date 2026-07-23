package com.quizapp.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public class TensionQuestionDto {

    private Long id;

    @NotBlank
    private String title;

    private String mainCategory;

    private String answersCategory;

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
