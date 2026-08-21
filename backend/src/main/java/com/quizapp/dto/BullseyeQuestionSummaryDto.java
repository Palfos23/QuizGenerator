package com.quizapp.dto;

public class BullseyeQuestionSummaryDto {

    private Long id;
    private String title;
    private String sport;
    private Integer targetValue;
    private String statLabel;
    private int entryCount;
    private boolean excludedFromBullseye;

    public BullseyeQuestionSummaryDto(Long id, String title, String sport, Integer targetValue,
                                       String statLabel, int entryCount, boolean excludedFromBullseye) {
        this.id = id;
        this.title = title;
        this.sport = sport;
        this.targetValue = targetValue;
        this.statLabel = statLabel;
        this.entryCount = entryCount;
        this.excludedFromBullseye = excludedFromBullseye;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getSport() {
        return sport;
    }

    public Integer getTargetValue() {
        return targetValue;
    }

    public String getStatLabel() {
        return statLabel;
    }

    public int getEntryCount() {
        return entryCount;
    }

    public boolean isExcludedFromBullseye() {
        return excludedFromBullseye;
    }
}
