package com.quizapp.dto;

public class AuthResponse {

    private String token;
    private String displayName;
    private String role; // USER or ADMIN

    public AuthResponse(String token, String displayName, String role) {
        this.token = token;
        this.displayName = displayName;
        this.role = role;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
