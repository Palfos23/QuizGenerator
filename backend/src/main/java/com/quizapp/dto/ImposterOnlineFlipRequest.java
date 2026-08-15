package com.quizapp.dto;

import jakarta.validation.constraints.NotNull;

public class ImposterOnlineFlipRequest {
    @NotNull
    private Long tileId;

    public Long getTileId() { return tileId; }
    public void setTileId(Long tileId) { this.tileId = tileId; }
}
