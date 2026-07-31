package com.quizapp.dto;

public class AthleteGridUsageDto {
    private Long gridId;
    private String gridTitle;
    private boolean isCorrectAnswer;

    public AthleteGridUsageDto(Long gridId, String gridTitle, boolean isCorrectAnswer) {
        this.gridId = gridId;
        this.gridTitle = gridTitle;
        this.isCorrectAnswer = isCorrectAnswer;
    }

    public Long getGridId() { return gridId; }
    public String getGridTitle() { return gridTitle; }
    public boolean isCorrectAnswer() { return isCorrectAnswer; }
}
