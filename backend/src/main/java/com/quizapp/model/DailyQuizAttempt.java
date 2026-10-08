package com.quizapp.model;

import jakarta.persistence.*;

import java.time.Instant;

// Table/column names kept as "weekly_quiz_attempts" - see DailyQuizSet's
// class comment for why (real production data already there).
@Entity
@Table(name = "weekly_quiz_attempts", uniqueConstraints = @UniqueConstraint(columnNames = {"set_id", "user_id"}))
public class DailyQuizAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "set_id", nullable = false)
    private DailyQuizSet set;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DailyQuizAttemptStatus status = DailyQuizAttemptStatus.IN_PROGRESS;

    // Only meaningful once status == GRADED - count of CORRECT answers.
    @Column(nullable = false)
    private int score = 0;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    // Same idea as GridAttempt.includeOnLeaderboard - lets a player keep their
    // own score without appearing on the shared leaderboard others see.
    //
    // columnDefinition carries an explicit default: this column was added
    // after weekly_quiz_attempts already had real rows in production (people
    // had already played), so ddl-auto=update's ALTER TABLE ... ADD COLUMN
    // needs a DEFAULT to backfill them - a bare "not null" with no default
    // is rejected outright by Postgres on a non-empty table, which is
    // exactly what took this feature down (column silently never created,
    // every later query 500ing on "column does not exist"). A brand-new
    // table's NOT NULL columns (like status/score above) never hit this,
    // since CREATE TABLE has no existing rows to conflict with.
    @Column(name = "include_on_leaderboard", nullable = false, columnDefinition = "boolean not null default true")
    private boolean includeOnLeaderboard = true;

    // A not-yet-submitted "save for later": the player's typed answers as a JSON object of
    // questionId -> text. Nullable on purpose (no default needed to add it to the populated table) and
    // cleared on submit - once submitted, the real DailyQuizAnswer rows are the record.
    @Column(name = "draft_answers", columnDefinition = "text")
    private String draftAnswers;

    public String getDraftAnswers() {
        return draftAnswers;
    }

    public void setDraftAnswers(String draftAnswers) {
        this.draftAnswers = draftAnswers;
    }

    // Set when an admin's review finishes (or changes) this attempt's score, so the player gets a "your
    // quiz has been reviewed" pop-up next time they visit; cleared once they've seen the result.
    //
    // columnDefinition carries the default for the same reason as includeOnLeaderboard above: this column
    // is added to a table that already has rows, and a bare NOT NULL would be rejected on Postgres.
    @Column(name = "review_result_unseen", nullable = false, columnDefinition = "boolean not null default false")
    private boolean reviewResultUnseen = false;

    public boolean isReviewResultUnseen() {
        return reviewResultUnseen;
    }

    public void setReviewResultUnseen(boolean reviewResultUnseen) {
        this.reviewResultUnseen = reviewResultUnseen;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public DailyQuizSet getSet() {
        return set;
    }

    public void setSet(DailyQuizSet set) {
        this.set = set;
    }

    public AppUser getUser() {
        return user;
    }

    public void setUser(AppUser user) {
        this.user = user;
    }

    public DailyQuizAttemptStatus getStatus() {
        return status;
    }

    public void setStatus(DailyQuizAttemptStatus status) {
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
