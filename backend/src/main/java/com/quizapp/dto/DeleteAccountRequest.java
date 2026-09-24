package com.quizapp.dto;

public class DeleteAccountRequest {
    // Only required for an account with a password (a Google-only account has
    // none to check - the JWT itself is proof enough there).
    private String password;

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
