package com.quizapp.dto;

public class GridCategoryDto {
    private Long id;
    private String name;
    private String groupLabel;

    public GridCategoryDto(Long id, String name, String groupLabel) {
        this.id = id;
        this.name = name;
        this.groupLabel = groupLabel;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getGroupLabel() { return groupLabel; }
}
