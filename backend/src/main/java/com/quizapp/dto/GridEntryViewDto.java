package com.quizapp.dto;

public class GridEntryViewDto {

    private Long id;
    private String hintLabel;
    private int hintValue;
    private boolean solved;
    private String athleteName; // null unless solved

    public GridEntryViewDto(Long id, String hintLabel, int hintValue, boolean solved, String athleteName) {
        this.id = id;
        this.hintLabel = hintLabel;
        this.hintValue = hintValue;
        this.solved = solved;
        this.athleteName = athleteName;
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
