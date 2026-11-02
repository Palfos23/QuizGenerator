package com.quizapp.model;

import jakarta.persistence.*;

/** A player's one setting for the advent calendar: whether their name may be listed on its leaderboard. No row = yes. */
@Entity
@Table(name = "advent_profiles")
public class AdventProfile {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "show_name", nullable = false)
    private boolean showName = true;

    public AdventProfile() {}

    public AdventProfile(Long userId, boolean showName) {
        this.userId = userId;
        this.showName = showName;
    }

    public Long getUserId() { return userId; }
    public boolean isShowName() { return showName; }
    public void setShowName(boolean showName) { this.showName = showName; }
}
