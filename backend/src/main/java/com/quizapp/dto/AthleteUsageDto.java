package com.quizapp.dto;

// Where a subject is used, across every game type that can reference one -
// shown to an admin before deleting a subject (see AthleteService#findUsage),
// so "this subject is used elsewhere" names the actual quiz instead of just
// failing. Replaces the old Grid-only AthleteGridUsageDto.
public class AthleteUsageDto {
    private String gameType; // "Grid", "Starting XI", "Bullseye", "501", "Imposter", "Penalty Shootout"
    private Long id;
    private String title;
    private boolean isCorrectAnswer;

    public AthleteUsageDto(String gameType, Long id, String title, boolean isCorrectAnswer) {
        this.gameType = gameType;
        this.id = id;
        this.title = title;
        this.isCorrectAnswer = isCorrectAnswer;
    }

    public String getGameType() { return gameType; }
    public Long getId() { return id; }
    public String getTitle() { return title; }
    public boolean isCorrectAnswer() { return isCorrectAnswer; }
}
