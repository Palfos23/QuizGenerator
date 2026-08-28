package com.quizapp.dto;

import java.time.Instant;
import java.util.List;

public class BullseyeRoundStateDto {

    private Long id;
    private String title;
    private String sport;
    private Integer targetValue;
    private String statLabel;
    private Instant updatedAt;
    private List<BullseyeEntryViewDto> entries;

    public BullseyeRoundStateDto(Long id, String title, String sport, Integer targetValue, String statLabel,
                                  Instant updatedAt, List<BullseyeEntryViewDto> entries) {
        this.id = id;
        this.title = title;
        this.sport = sport;
        this.targetValue = targetValue;
        this.statLabel = statLabel;
        this.updatedAt = updatedAt;
        this.entries = entries;
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

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public List<BullseyeEntryViewDto> getEntries() {
        return entries;
    }
}
