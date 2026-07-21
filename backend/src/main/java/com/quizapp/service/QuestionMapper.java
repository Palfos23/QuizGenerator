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
        return question;
    }
}
