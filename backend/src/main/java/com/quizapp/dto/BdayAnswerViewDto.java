package com.quizapp.dto;

/** One line of a finished guest's answer sheet, for the admin. */
public class BdayAnswerViewDto {

    private String prompt;
    private String type; // MAP | STATEMENT | PHOTO | TEXT
    private String response;
    private int points;
    private int maxPoints;

    public BdayAnswerViewDto(String prompt, String type, String response, int points, int maxPoints) {
        this.prompt = prompt;
        this.type = type;
        this.response = response;
        this.points = points;
        this.maxPoints = maxPoints;
    }

    public String getPrompt() { return prompt; }
    public String getType() { return type; }
    public String getResponse() { return response; }
    public int getPoints() { return points; }
    public int getMaxPoints() { return maxPoints; }
}
