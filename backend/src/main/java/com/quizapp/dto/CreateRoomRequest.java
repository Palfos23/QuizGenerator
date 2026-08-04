package com.quizapp.dto;

import com.quizapp.model.RoomGameType;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class CreateRoomRequest {
    @NotNull
    private RoomGameType gameType;

    private String displayName;
    private String color;

    // Grid Battle only - either an explicit list of grid ids, or leave null/empty
    // and set randomCount to have the server pick that many at random.
    private List<Long> gridIds;
    private Integer randomGridCount;

    // Tension only.
    private Integer tensionNumQuestions;
    private String tensionCategory;
    private List<String> tensionExcludeCategories;

    public Integer getTensionNumQuestions() { return tensionNumQuestions; }
    public void setTensionNumQuestions(Integer tensionNumQuestions) { this.tensionNumQuestions = tensionNumQuestions; }
    public String getTensionCategory() { return tensionCategory; }
    public void setTensionCategory(String tensionCategory) { this.tensionCategory = tensionCategory; }
    public List<String> getTensionExcludeCategories() { return tensionExcludeCategories; }
    public void setTensionExcludeCategories(List<String> tensionExcludeCategories) { this.tensionExcludeCategories = tensionExcludeCategories; }

    public RoomGameType getGameType() { return gameType; }
    public void setGameType(RoomGameType gameType) { this.gameType = gameType; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public List<Long> getGridIds() { return gridIds; }
    public void setGridIds(List<Long> gridIds) { this.gridIds = gridIds; }
    public Integer getRandomGridCount() { return randomGridCount; }
    public void setRandomGridCount(Integer randomGridCount) { this.randomGridCount = randomGridCount; }
}
