package com.quizapp.dto;

public class ImposterOnlineRevealDto {
    private Long tileId;
    private String imposterName;
    private String replacedName;
    private String flippedByName;

    public ImposterOnlineRevealDto(Long tileId, String imposterName, String replacedName, String flippedByName) {
        this.tileId = tileId;
        this.imposterName = imposterName;
        this.replacedName = replacedName;
        this.flippedByName = flippedByName;
    }

    public Long getTileId() { return tileId; }
    public String getImposterName() { return imposterName; }
    public String getReplacedName() { return replacedName; }
    public String getFlippedByName() { return flippedByName; }
}
