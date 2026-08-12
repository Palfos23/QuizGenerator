package com.quizapp.dto;

public class ImposterFlipResultDto {
    private Long tileId;
    private boolean imposter;
    private String revealPhotoUrl;

    public ImposterFlipResultDto(Long tileId, boolean imposter, String revealPhotoUrl) {
        this.tileId = tileId;
        this.imposter = imposter;
        this.revealPhotoUrl = revealPhotoUrl;
    }

    public Long getTileId() {
        return tileId;
    }

    public boolean isImposter() {
        return imposter;
    }

    public String getRevealPhotoUrl() {
        return revealPhotoUrl;
    }
}
