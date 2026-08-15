package com.quizapp.dto;

import jakarta.validation.constraints.NotNull;

public class FiveOhOneThrowRequest {
    @NotNull
    private Long entryId;

    public Long getEntryId() { return entryId; }
    public void setEntryId(Long entryId) { this.entryId = entryId; }
}
