package com.quizapp.dto;

import com.quizapp.model.Language;

public class CategoryStatDto {

    private String category;
    private Language language;
    private long count;
    private int minDifficulty;
    private int maxDifficulty;

    public CategoryStatDto(String category, Language language, long count, int minDifficulty, int maxDifficulty) {
        this.category = category;
        this.language = language;
        this.count = count;
        this.minDifficulty = minDifficulty;
        this.maxDifficulty = maxDifficulty;
    }

    public String getCategory() {
        return category;
    }

    public Language getLanguage() {
        return language;
    }

    public long getCount() {
        return count;
    }

    public int getMinDifficulty() {
        return minDifficulty;
    }

    public int getMaxDifficulty() {
        return maxDifficulty;
    }
}
