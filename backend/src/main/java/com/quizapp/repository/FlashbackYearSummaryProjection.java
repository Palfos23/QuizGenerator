package com.quizapp.repository;

public interface FlashbackYearSummaryProjection {
    Long getId();
    String getTitle();
    Integer getYear();
    Long getHintCount();
    Boolean getExcludedFromFlashback();
    Boolean getCanExpire();
    java.time.Instant getUpdatedAt();
}
