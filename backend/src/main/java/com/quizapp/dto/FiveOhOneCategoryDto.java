package com.quizapp.dto;

import java.time.Instant;
import java.util.List;

public class FiveOhOneCategoryDto {
    private Long id;
    private String title;
    private String description;
    private Instant updatedAt;
    private List<FiveOhOneEntryDto> entries;

    public FiveOhOneCategoryDto() {
    }

    public FiveOhOneCategoryDto(Long id, String title, String description, List<FiveOhOneEntryDto> entries) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.entries = entries;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public List<FiveOhOneEntryDto> getEntries() { return entries; }
    public void setEntries(List<FiveOhOneEntryDto> entries) { this.entries = entries; }
}
