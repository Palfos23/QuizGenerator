package com.quizapp.dto;

import java.time.Instant;

public class BullseyeQuestionSummaryDto {

    private Long id;
    private String title;
    private String sport;
    private Integer targetValue;
    private String statLabel;
    private int entryCount;
    private boolean excludedFromBullseye;
    private boolean canExpire;
    private boolean entireCategoryPool;
    private boolean groupDigits;
    private Instant updatedAt;

    public BullseyeQuestionSummaryDto(Long id, String title, String sport, Integer targetValue,
                                       String statLabel, int entryCount, boolean excludedFromBullseye,
                                       boolean canExpire, boolean entireCategoryPool, boolean groupDigits,
                                       Instant updatedAt) {
        this.id = id;
        this.title = title;
        this.sport = sport;
        this.targetValue = targetValue;
        this.statLabel = statLabel;
        this.entryCount = entryCount;
        this.excludedFromBullseye = excludedFromBullseye;
        this.canExpire = canExpire;
        this.entireCategoryPool = entireCategoryPool;
        this.groupDigits = groupDigits;
        this.updatedAt = updatedAt;
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

    public boolean isCanExpire() {
        return canExpire;
    }

    public boolean isEntireCategoryPool() {
        return entireCategoryPool;
    }

    public boolean isGroupDigits() {
        return groupDigits;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
