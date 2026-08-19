package com.quizapp.dto;

import jakarta.validation.constraints.NotNull;

public class LineupBattleChooseRequest {
    @NotNull
    private Long lineupId;

    public Long getLineupId() { return lineupId; }
    public void setLineupId(Long lineupId) { this.lineupId = lineupId; }
}
