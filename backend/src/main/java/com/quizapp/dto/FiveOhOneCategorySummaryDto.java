package com.quizapp.dto;

public class FiveOhOneCategorySummaryDto {
    private Long id;
    private String title;
    private String description;
    private int entryCount;
    private boolean canExpire;

    public FiveOhOneCategorySummaryDto(Long id, String title, String description, int entryCount, boolean canExpire) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.entryCount = entryCount;
        this.canExpire = canExpire;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public int getEntryCount() { return entryCount; }
    public boolean isCanExpire() { return canExpire; }
}
