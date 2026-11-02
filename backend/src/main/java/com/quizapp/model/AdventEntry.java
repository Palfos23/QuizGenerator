package com.quizapp.model;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * One player's four answers for one door - what they sent in, plus their two choices (risk, joker). Points are never
 * stored: they are worked out from this and the game master's answers every time (see AdventScoring), so a correction
 * by the game master changes everyone's points at once.
 *
 * Keyed by user id with no foreign key, like DailyQuizResult; AccountService removes a person's rows when they delete
 * their account.
 */
@Entity
@Table(name = "advent_entries",
        uniqueConstraints = @UniqueConstraint(name = "uq_advent_entries_day_user", columnNames = {"advent_day_id", "user_id"}))
public class AdventEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "advent_day_id", nullable = false)
    private Long dayId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    // The four answers, one per line.
    @Column(name = "answers_text", nullable = false, length = 1000)
    private String answersText;

    // Only means something on a RISK_REWARD door.
    @Column(nullable = false)
    private boolean risky = false;

    @Column(name = "joker_used", nullable = false)
    private boolean jokerUsed = false;

    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt = Instant.now();

    public List<String> getAnswers() {
        return new ArrayList<>(Arrays.asList(answersText.split("\n", -1)));
    }

    public void setAnswers(List<String> answers) {
        this.answersText = String.join("\n", answers);
    }

    public Long getId() { return id; }
    public Long getDayId() { return dayId; }
    public void setDayId(Long dayId) { this.dayId = dayId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public boolean isRisky() { return risky; }
    public void setRisky(boolean risky) { this.risky = risky; }
    public boolean isJokerUsed() { return jokerUsed; }
    public void setJokerUsed(boolean jokerUsed) { this.jokerUsed = jokerUsed; }
    public Instant getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(Instant submittedAt) { this.submittedAt = submittedAt; }
}
