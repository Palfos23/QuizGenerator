package com.quizapp.service;

import com.quizapp.dto.QuestionDto;
import com.quizapp.model.Question;

public final class QuestionMapper {

    private QuestionMapper() {
    }

    public static QuestionDto toDto(Question question) {
        QuestionDto dto = new QuestionDto();
        dto.setId(question.getId());
        dto.setQuestionText(question.getQuestionText());
        dto.setCategory(question.getCategory());
        dto.setDifficultyLevel(question.getDifficultyLevel());
        dto.setLanguage(question.getLanguage());
        dto.setAnswer(question.getAnswer());
        dto.setCouldChange(question.isCouldChange());
        dto.setPhotoUrl(question.getPhotoUrl());
        dto.setLabelIds(question.getLabels().stream().map(com.quizapp.model.QuestionLabel::getId).collect(java.util.stream.Collectors.toList()));
        dto.setLabelNames(question.getLabels().stream().map(com.quizapp.model.QuestionLabel::getName).sorted().collect(java.util.stream.Collectors.toList()));
        return dto;
    }

    public static Question toEntity(QuestionDto dto) {
        Question question = new Question();
        question.setId(dto.getId());
        question.setQuestionText(dto.getQuestionText());
        question.setCategory(dto.getCategory());
        question.setDifficultyLevel(dto.getDifficultyLevel());
        question.setLanguage(dto.getLanguage());
        question.setAnswer(dto.getAnswer());
        question.setCouldChange(dto.isCouldChange());
        question.setPhotoUrl(dto.getPhotoUrl());
        return question;
    }
}
