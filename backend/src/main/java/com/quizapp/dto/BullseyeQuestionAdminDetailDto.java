package com.quizapp.dto;

import java.util.List;

public class BullseyeQuestionAdminDetailDto {

    private Long id;
    private String title;
    private String sport;
    private Integer targetValue;
    private String statLabel;
    private boolean excludedFromBullseye;
    private boolean entireCategoryPool;
    private List<EntryDetail> entries;

    public static class EntryDetail {
        private Long id;
        private AthleteDto athlete;
        private Integer statValue;

        public EntryDetail(Long id, AthleteDto athlete, Integer statValue) {
            this.id = id;
            this.athlete = athlete;
            this.statValue = statValue;
        }

        public Long getId() {
            return id;
        }

        public AthleteDto getAthlete() {
            return athlete;
        }

        public Integer getStatValue() {
            return statValue;
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSport() {
        return sport;
    }

    public void setSport(String sport) {
        this.sport = sport;
    }

    public Integer getTargetValue() {
        return targetValue;
    }

    public void setTargetValue(Integer targetValue) {
        this.targetValue = targetValue;
    }

    public String getStatLabel() {
        return statLabel;
    }

    public void setStatLabel(String statLabel) {
        this.statLabel = statLabel;
    }

    public boolean isExcludedFromBullseye() {
        return excludedFromBullseye;
    }

    public void setExcludedFromBullseye(boolean excludedFromBullseye) {
        this.excludedFromBullseye = excludedFromBullseye;
    }

    public boolean isEntireCategoryPool() {
        return entireCategoryPool;
    }

    public void setEntireCategoryPool(boolean entireCategoryPool) {
        this.entireCategoryPool = entireCategoryPool;
    }

    public List<EntryDetail> getEntries() {
        return entries;
    }

    public void setEntries(List<EntryDetail> entries) {
        this.entries = entries;
    }
}
