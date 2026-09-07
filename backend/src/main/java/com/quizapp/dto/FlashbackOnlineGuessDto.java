package com.quizapp.dto;

// One guess made so far this round - visible to everyone as soon as it's
// submitted (the guessed year, not whether it's right - see
// FlashbackOnlineService for why no correctness signal leaks per-guess).
public class FlashbackOnlineGuessDto {
    private String name;
    private int year;
    private int hintIndex;

    public FlashbackOnlineGuessDto(String name, int year, int hintIndex) {
        this.name = name;
        this.year = year;
        this.hintIndex = hintIndex;
    }

    public String getName() { return name; }
    public int getYear() { return year; }
    public int getHintIndex() { return hintIndex; }
}
