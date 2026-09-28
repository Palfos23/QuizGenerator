package com.quizapp.model;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "weekly_quiz_attempts", uniqueConstraints = @UniqueConstraint(columnNames = {"set_id", "user_id"}))
public class WeeklyQuizAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "set_id", nullable = false)
    private WeeklyQuizSet set;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WeeklyQuizAttemptStatus status = WeeklyQuizAttemptStatus.IN_PROGRESS;

    // Only meaningful once status == GRADED - count of CORRECT answers.
    @Column(nullable = false)
    private int score = 0;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    // Same idea as GridAttempt.includeOnLeaderboard - lets a player keep their
    // own score without appearing on the shared leaderboard others see.
    @Column(name = "include_on_leaderboard", nullable = false)
    private boolean includeOnLeaderboard = true;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public WeeklyQuizSet getSet() {
        return set;
    }

    public void setSet(WeeklyQuizSet set) {
        this.set = set;
    }

    public AppUser getUser() {
        return user;
    }

    public void setUser(AppUser user) {
        this.user = user;
    }

    public WeeklyQuizAttemptStatus getStatus() {
        return status;
    }

    public void setStatus(WeeklyQuizAttemptStatus status) {
        this.status = status;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(Instant submittedAt) {
        this.submittedAt = submittedAt;
    }

    public boolean isIncludeOnLeaderboard() {
        return includeOnLeaderboard;
    }

    public void setIncludeOnLeaderboard(boolean includeOnLeaderboard) {
        this.includeOnLeaderboard = includeOnLeaderboard;
    }
}
