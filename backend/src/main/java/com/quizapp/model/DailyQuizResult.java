package com.quizapp.model;

import jakarta.persistence.*;

import java.time.LocalDate;

/**
 * One player's final score for one day's daily quiz - the compact record that outlives the quiz.
 *
 * A quiz (its questions, and every player's attempt and per-question answers) is deleted once it's 7
 * days old, to keep storage small - but the weekly winner is worked out from SEVERAL days' scores, and
 * a week's first day is already 7 days old by the time it ends. So the result of each graded attempt
 * is copied here (one small row per player per day) when it's graded, and the weekly standings are
 * calculated from these rows alone. Rows are pruned long after the quizzes themselves are gone.
 *
 * Keyed by user id with no foreign key on purpose, so the row can outlive anything else about the
 * quiz; AccountService removes a person's rows explicitly when they delete their account.
 */
@Entity
@Table(name = "daily_quiz_results",
        uniqueConstraints = @UniqueConstraint(name = "uq_daily_quiz_results_user_day", columnNames = {"user_id", "quiz_date"}))
public class DailyQuizResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "quiz_date", nullable = false)
    private LocalDate quizDate;

    // Name as of when it was graded - so the weekly standings still read right after the quiz is gone.
    @Column(name = "player_name", nullable = false)
    private String playerName;

    @Column(nullable = false)
    private int score;

    @Column(name = "max_score", nullable = false)
    private int maxScore;

    // Copied from the attempt's "show me on the leaderboard" choice: a player who opted out stays off
    // the weekly standings and can't be named as the weekly winner, same as on the daily scoreboard.
    @Column(name = "include_on_leaderboard", nullable = false)
    private boolean includeOnLeaderboard = true;

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public LocalDate getQuizDate() { return quizDate; }
    public void setQuizDate(LocalDate quizDate) { this.quizDate = quizDate; }
    public String getPlayerName() { return playerName; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }
    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }
    public int getMaxScore() { return maxScore; }
    public void setMaxScore(int maxScore) { this.maxScore = maxScore; }
    public boolean isIncludeOnLeaderboard() { return includeOnLeaderboard; }
    public void setIncludeOnLeaderboard(boolean includeOnLeaderboard) { this.includeOnLeaderboard = includeOnLeaderboard; }
}
