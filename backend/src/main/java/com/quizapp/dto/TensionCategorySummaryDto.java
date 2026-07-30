package com.quizapp.dto;

public class TensionCategorySummaryDto {
    private Long id;
    private String name;
    private int optionCount;

    public TensionCategorySummaryDto(Long id, String name, int optionCount) {
        this.id = id;
        this.name = name;
        this.optionCount = optionCount;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public int getOptionCount() { return optionCount; }
}
