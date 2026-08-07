package com.quizapp.dto;

import jakarta.validation.constraints.NotBlank;

public class GridCategoryRequest {
    @NotBlank
    private String name;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
