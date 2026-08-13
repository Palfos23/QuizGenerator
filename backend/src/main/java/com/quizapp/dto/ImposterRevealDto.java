package com.quizapp.dto;

public class ImposterRevealDto {
    private Long tileId;
    private String imposterName;
    private String replacedName; // null if the admin didn't set one for this imposter

    public ImposterRevealDto(Long tileId, String imposterName, String replacedName) {
        this.tileId = tileId;
        this.imposterName = imposterName;
        this.replacedName = replacedName;
    }

    public Long getTileId() {
        return tileId;
    }

    public String getImposterName() {
        return imposterName;
    }

    public String getReplacedName() {
        return replacedName;
    }
}
