package com.quizapp.dto;

public class ImposterFlipResultDto {
    private Long tileId;
    private boolean imposter;

    public ImposterFlipResultDto(Long tileId, boolean imposter) {
        this.tileId = tileId;
        this.imposter = imposter;
    }

    public Long getTileId() {
        return tileId;
    }

    public boolean isImposter() {
        return imposter;
    }
}
