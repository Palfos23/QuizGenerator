package com.quizapp.repository;

public interface FlashbackYearSummaryProjection {
    Long getId();
    String getTitle();
    String getCategory();
    Integer getYear();
    Long getHintCount();
    Boolean getExcludedFromFlashback();
    java.time.Instant getUpdatedAt();
}
