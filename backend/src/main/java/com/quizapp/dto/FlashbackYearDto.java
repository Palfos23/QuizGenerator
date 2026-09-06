package com.quizapp.dto;

import java.time.Instant;
import java.util.List;

// Dual-purpose, like TensionQuestionDto: the admin's single-get/create/update
// response, AND the player-facing round-choices/round-start payload - the
// full year + hint list is fair game the moment a round is offered, same
// trust model as Bullseye/Tension (see FlashbackYear's class comment).
public class FlashbackYearDto {

    private Long id;
    private String title;
    private Integer year;
    private List<String> hints;
    private boolean excludedFromFlashback;
    private Instant updatedAt;

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

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
