package com.quizapp.dto;

import jakarta.validation.constraints.NotNull;

public class ImposterChooseRequest {
    @NotNull
    private Long gridId;

    public Long getGridId() { return gridId; }
    public void setGridId(Long gridId) { this.gridId = gridId; }
}
