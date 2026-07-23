package com.quizapp.dto;

public class GridEntryViewDto {

    private Long id;
    private String hintLabel;
    private int hintValue;
    private boolean solved;
    private boolean guessedByUser; // true only if this specific entry was actually guessed correctly - not just revealed
    private String athleteName; // null unless solved
    private String athletePhotoUrl; // null unless solved - showing it earlier would let players recognize the athlete before guessing
    private String logoUrl; // null if no club set, or the admin hid it for this entry
    private String hintColor; // the entry's club's color, if any set - null falls back to the app default gold

    public GridEntryViewDto(Long id, String hintLabel, int hintValue, boolean solved, boolean guessedByUser,
                             String athleteName, String athletePhotoUrl, String logoUrl, String hintColor) {
        this.id = id;
        this.hintLabel = hintLabel;
        this.hintValue = hintValue;
        this.solved = solved;
        this.guessedByUser = guessedByUser;
        this.athleteName = athleteName;
        this.athletePhotoUrl = athletePhotoUrl;
        this.logoUrl = logoUrl;
        this.hintColor = hintColor;
    }

    public boolean isGuessedByUser() {
        return guessedByUser;
    }

    public String getAthletePhotoUrl() {
        return athletePhotoUrl;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public String getHintColor() {
        return hintColor;
    }

    public Long getId() {
        return id;
    }

    public String getHintLabel() {
        return hintLabel;
    }

    public int getHintValue() {
        return hintValue;
    }

    public boolean isSolved() {
        return solved;
    }

    public String getAthleteName() {
        return athleteName;
    }
}
