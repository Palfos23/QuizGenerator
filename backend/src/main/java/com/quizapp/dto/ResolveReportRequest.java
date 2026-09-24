package com.quizapp.dto;

import jakarta.validation.constraints.Size;

public class ResolveReportRequest {

    @Size(max = 2000)
    private String adminNote;

    public String getAdminNote() {
        return adminNote;
    }

    public void setAdminNote(String adminNote) {
        this.adminNote = adminNote;
    }
}
