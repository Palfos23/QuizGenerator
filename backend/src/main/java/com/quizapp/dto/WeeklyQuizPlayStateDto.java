package com.quizapp.dto;

import java.time.LocalDate;
import java.util.List;

public class WeeklyQuizPlayStateDto {

    private LocalDate weekStartDate;
    private String attemptStatus; // IN_PROGRESS / SUBMITTED / GRADED
    private List<QuestionDto> questions;
    private WeeklyQuizResultDto result; // only set once attemptStatus == GRADED

    public LocalDate getWeekStartDate() {
        return weekStartDate;
    }

    public void setWeekStartDate(LocalDate weekStartDate) {
        this.weekStartDate = weekStartDate;
    }

    public String getAttemptStatus() {
        return attemptStatus;
    }

    public void setAttemptStatus(String attemptStatus) {
        this.attemptStatus = attemptStatus;
    }

    public List<QuestionDto> getQuestions() {
        return questions;
    }

    public void setQuestions(List<QuestionDto> questions) {
        this.questions = questions;
    }

    public WeeklyQuizResultDto getResult() {
        return result;
    }

    public void setResult(WeeklyQuizResultDto result) {
        this.result = result;
    }

    // Never carries the answer - that stays server-side until GRADED.
    public static class QuestionDto {
        private int questionNumber;
        private Long questionId;
        private String text;

        public QuestionDto() {
        }

        public QuestionDto(int questionNumber, Long questionId, String text) {
            this.questionNumber = questionNumber;
            this.questionId = questionId;
            this.text = text;
        }

        public int getQuestionNumber() {
            return questionNumber;
        }

        public void setQuestionNumber(int questionNumber) {
            this.questionNumber = questionNumber;
        }

        public Long getQuestionId() {
            return questionId;
        }

        public void setQuestionId(Long questionId) {
            this.questionId = questionId;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }
    }
}
