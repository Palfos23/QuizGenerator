package com.quizapp.dto;

public class GridBattleEntryDto {
    private Long id;
    private String hintLabel;
    private int hintValue;
    private boolean solved;
    private String athleteName;
    private String athletePhotoUrl;
    private String logoUrl;
    private String hintColor;
    private String solvedByName;

    public GridBattleEntryDto(Long id, String hintLabel, int hintValue, boolean solved, String athleteName,
                               String athletePhotoUrl, String logoUrl, String hintColor, String solvedByName) {
        this.id = id;
        this.hintLabel = hintLabel;
        this.hintValue = hintValue;
        this.solved = solved;
        this.athleteName = athleteName;
        this.athletePhotoUrl = athletePhotoUrl;
        this.logoUrl = logoUrl;
        this.hintColor = hintColor;
        this.solvedByName = solvedByName;
    }

    public Long getId() { return id; }
    public String getHintLabel() { return hintLabel; }
    public int getHintValue() { return hintValue; }
    public boolean isSolved() { return solved; }
    public String getAthleteName() { return athleteName; }
    public String getAthletePhotoUrl() { return athletePhotoUrl; }
    public String getLogoUrl() { return logoUrl; }
    public String getHintColor() { return hintColor; }
    public String getSolvedByName() { return solvedByName; }
}
