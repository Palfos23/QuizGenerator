package com.quizapp.dto;

import java.time.Instant;

public class BdayGuestDto {
    private Long id;
    private String name;
    private String status;
    private int score;
    private Instant completedAt;

    public BdayGuestDto() {
    }

    public BdayGuestDto(Long id, String name, String status, int score, Instant completedAt) {
        this.id = id;
        this.name = name;
        this.status = status;
        this.score = score;
        this.completedAt = completedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }
}
