package com.quizapp.dto;

import java.time.LocalDate;
import java.util.List;

public class DailyQuizPlayStateDto {

    private Long setId;
    private LocalDate quizDate;
    private String attemptStatus; // IN_PROGRESS / SUBMITTED / GRADED
    private List<QuestionDto> questions; // only set while attemptStatus == IN_PROGRESS (the form to fill in)
    private DailyQuizResultDto result; // set once attemptStatus is SUBMITTED or GRADED (see DailyQuizResultDto)

    public Long getSetId() {
        return setId;
    }

    public void setSetId(Long setId) {
        this.setId = setId;
    }

    public LocalDate getQuizDate() {
        return quizDate;
    }

    public void setQuizDate(LocalDate quizDate) {
        this.quizDate = quizDate;
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

    public DailyQuizResultDto getResult() {
        return result;
    }

    public void setResult(DailyQuizResultDto result) {
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
