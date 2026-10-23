package com.quizapp.dto;

public class BdayQuestionAdminDto {

    private Long id;
    private String type;
    private String prompt;
    private String photoUrl;
    private String correctOption;
    private String acceptedAnswers;

    public BdayQuestionAdminDto(Long id, String type, String prompt, String photoUrl, String correctOption, String acceptedAnswers) {
        this.id = id;
        this.type = type;
        this.prompt = prompt;
        this.photoUrl = photoUrl;
        this.correctOption = correctOption;
        this.acceptedAnswers = acceptedAnswers;
    }

    public Long getId() { return id; }
    public String getType() { return type; }
    public String getPrompt() { return prompt; }
    public String getPhotoUrl() { return photoUrl; }
    public String getCorrectOption() { return correctOption; }
    public String getAcceptedAnswers() { return acceptedAnswers; }
}
