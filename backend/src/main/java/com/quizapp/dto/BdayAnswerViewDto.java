package com.quizapp.dto;

/** One line of a finished guest's answer sheet, for the admin. */
public class BdayAnswerViewDto {

    private Long id;
    private String prompt;
    private String type; // MAP | STATEMENT | PHOTO | TEXT
    private String response;
    private String correctAnswer;
    private int points;
    private int maxPoints;

    public BdayAnswerViewDto(Long id, String prompt, String type, String response, String correctAnswer, int points, int maxPoints) {
        this.id = id;
        this.prompt = prompt;
        this.type = type;
        this.response = response;
        this.correctAnswer = correctAnswer;
        this.points = points;
        this.maxPoints = maxPoints;
    }

    public Long getId() { return id; }
    public String getCorrectAnswer() { return correctAnswer; }
    public String getPrompt() { return prompt; }
    public String getType() { return type; }
    public String getResponse() { return response; }
    public int getPoints() { return points; }
    public int getMaxPoints() { return maxPoints; }
}
