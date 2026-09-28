package com.quizapp.dto;

public class WeeklyQuizPendingAnswerDto {

    private Long id;
    private String playerName;
    private String questionText;
    private String correctAnswer;
    private String submittedAnswer;

    public WeeklyQuizPendingAnswerDto() {
    }

    public WeeklyQuizPendingAnswerDto(Long id, String playerName, String questionText, String correctAnswer, String submittedAnswer) {
        this.id = id;
        this.playerName = playerName;
        this.questionText = questionText;
        this.correctAnswer = correctAnswer;
        this.submittedAnswer = submittedAnswer;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public String getQuestionText() {
        return questionText;
    }

    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }

    public String getCorrectAnswer() {
        return correctAnswer;
    }

    public void setCorrectAnswer(String correctAnswer) {
        this.correctAnswer = correctAnswer;
    }

    public String getSubmittedAnswer() {
        return submittedAnswer;
    }

    public void setSubmittedAnswer(String submittedAnswer) {
        this.submittedAnswer = submittedAnswer;
    }
}
