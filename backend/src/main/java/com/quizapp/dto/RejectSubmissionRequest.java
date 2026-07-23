package com.quizapp.dto;

import jakarta.validation.constraints.NotBlank;

public class RejectSubmissionRequest {

    @NotBlank
    private String reason;

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
