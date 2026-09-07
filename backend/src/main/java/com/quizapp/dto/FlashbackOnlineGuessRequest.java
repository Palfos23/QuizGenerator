package com.quizapp.dto;

import jakarta.validation.constraints.NotNull;

public class FlashbackOnlineGuessRequest {
    @NotNull
    private Integer year;

    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }
}
