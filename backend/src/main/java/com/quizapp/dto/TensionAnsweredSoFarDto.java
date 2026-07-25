package com.quizapp.dto;

public class TensionAnsweredSoFarDto {
    private String name;
    private String answerText;

    public TensionAnsweredSoFarDto(String name, String answerText) {
        this.name = name;
        this.answerText = answerText;
    }

    public String getName() { return name; }
    public String getAnswerText() { return answerText; }
}
