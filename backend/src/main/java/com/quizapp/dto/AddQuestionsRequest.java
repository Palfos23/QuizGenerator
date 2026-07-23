package com.quizapp.dto;

import com.quizapp.model.Language;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class AddQuestionsRequest {

    @NotBlank
    private String category;

    @NotNull
    private Language language;

    @Min(1) @Max(10)
    private Integer minDifficulty;

    @Min(1) @Max(10)
    private Integer maxDifficulty;

    @Min(1)
    private int count;

    private List<Long> excludeIds;

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Language getLanguage() {
        return language;
    }

    public void setLanguage(Language language) {
        this.language = language;
    }

    public Integer getMinDifficulty() {
        return minDifficulty;
    }

    public void setMinDifficulty(Integer minDifficulty) {
        this.minDifficulty = minDifficulty;
    }

    public Integer getMaxDifficulty() {
        return maxDifficulty;
    }

    public void setMaxDifficulty(Integer maxDifficulty) {
        this.maxDifficulty = maxDifficulty;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public List<Long> getExcludeIds() {
        return excludeIds;
    }

    public void setExcludeIds(List<Long> excludeIds) {
        this.excludeIds = excludeIds;
    }
}
