package com.quizapp.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public class FiveOhOneCategoryRequest {
    @NotBlank
    private String title;

    private String description;

    private boolean canExpire;

    @NotEmpty
    @Valid
    private List<FiveOhOneEntryDto> entries;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public boolean isCanExpire() { return canExpire; }
    public void setCanExpire(boolean canExpire) { this.canExpire = canExpire; }
    public List<FiveOhOneEntryDto> getEntries() { return entries; }
    public void setEntries(List<FiveOhOneEntryDto> entries) { this.entries = entries; }
}
