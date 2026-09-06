package com.quizapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class FlashbackYearRequest {

    @NotBlank
    private String title;

    @NotNull
    private Integer year;

    @NotEmpty
    private List<String> hints;

    private boolean excludedFromFlashback = false;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public List<String> getHints() {
        return hints;
    }

    public void setHints(List<String> hints) {
        this.hints = hints;
    }

    public boolean isExcludedFromFlashback() {
        return excludedFromFlashback;
    }

    public void setExcludedFromFlashback(boolean excludedFromFlashback) {
        this.excludedFromFlashback = excludedFromFlashback;
    }
}
