package com.quizapp.dto;

import jakarta.validation.constraints.NotBlank;

public class GridCategoryRequest {
    @NotBlank
    private String name;

    // Optional - defaults to "Team" in the service layer if left blank.
    private String groupLabel;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getGroupLabel() { return groupLabel; }
    public void setGroupLabel(String groupLabel) { this.groupLabel = groupLabel; }
}
