package com.quizapp.dto;

import java.time.Instant;

public class FiveOhOneCategorySummaryDto {
    private Long id;
    private String title;
    private String description;
    private int entryCount;
    private boolean canExpire;
    private Instant updatedAt;

    public FiveOhOneCategorySummaryDto(Long id, String title, String description, int entryCount, boolean canExpire,
                                        Instant updatedAt) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.entryCount = entryCount;
        this.canExpire = canExpire;
        this.updatedAt = updatedAt;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public int getEntryCount() { return entryCount; }
    public boolean isCanExpire() { return canExpire; }
    public Instant getUpdatedAt() { return updatedAt; }
}
