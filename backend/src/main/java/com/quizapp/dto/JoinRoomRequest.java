package com.quizapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class JoinRoomRequest {
    @NotBlank
    @Size(max = 40)
    private String displayName;
    private String color;

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
}
