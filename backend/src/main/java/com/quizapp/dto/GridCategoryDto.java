package com.quizapp.dto;

public class GridCategoryDto {
    private Long id;
    private String name;

    public GridCategoryDto(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
}
