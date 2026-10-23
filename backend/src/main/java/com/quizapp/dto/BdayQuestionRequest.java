package com.quizapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** The admin's version of a question - the correct answer included. */
public class BdayQuestionRequest {

    @NotBlank
    private String type; // STATEMENT | PHOTO | TEXT
    @Size(max = 500)
    private String prompt;
    @Size(max = 1000)
    private String photoUrl;
    private String correctOption; // PAL | ERIK | BEGGE | INGEN
    @Size(max = 2000)
    private String acceptedAnswers; // one per line

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }
    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }
    public String getCorrectOption() { return correctOption; }
    public void setCorrectOption(String correctOption) { this.correctOption = correctOption; }
    public String getAcceptedAnswers() { return acceptedAnswers; }
    public void setAcceptedAnswers(String acceptedAnswers) { this.acceptedAnswers = acceptedAnswers; }
}
