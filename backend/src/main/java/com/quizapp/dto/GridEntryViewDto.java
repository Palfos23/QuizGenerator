package com.quizapp.dto;

public class GridEntryViewDto {

    private Long id;
    private String hintLabel;
    private int hintValue;
    private boolean solved;
    private String athleteName; // null unless solved
    private String logoUrl; // null if no club set, or the admin hid it for this entry

    public GridEntryViewDto(Long id, String hintLabel, int hintValue, boolean solved, String athleteName, String logoUrl) {
        this.id = id;
        this.hintLabel = hintLabel;
        this.hintValue = hintValue;
        this.solved = solved;
        this.athleteName = athleteName;
        this.logoUrl = logoUrl;
    }

    public String getLogoUrl() {
        return logoUrl;
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
