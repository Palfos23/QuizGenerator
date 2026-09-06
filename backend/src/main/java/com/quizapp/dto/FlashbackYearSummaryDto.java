package com.quizapp.dto;

import java.time.Instant;

public class FlashbackYearSummaryDto {

    private Long id;
    private String title;
    private Integer year;
    private int hintCount;
    private boolean excludedFromFlashback;
    private Instant updatedAt;

    public FlashbackYearSummaryDto(Long id, String title, Integer year, int hintCount,
                                    boolean excludedFromFlashback, Instant updatedAt) {
        this.id = id;
        this.title = title;
        this.year = year;
        this.hintCount = hintCount;
        this.excludedFromFlashback = excludedFromFlashback;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public Integer getYear() {
        return year;
    }

    public int getHintCount() {
        return hintCount;
    }

    public boolean isExcludedFromFlashback() {
        return excludedFromFlashback;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
