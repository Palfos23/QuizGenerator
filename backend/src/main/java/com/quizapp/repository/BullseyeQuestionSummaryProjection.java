package com.quizapp.repository;

public interface BullseyeQuestionSummaryProjection {
    Long getId();
    String getTitle();
    String getSport();
    Integer getTargetValue();
    String getStatLabel();
    Long getEntryCount();
    Boolean getExcludedFromBullseye();
    Boolean getEntireCategoryPool();
}
