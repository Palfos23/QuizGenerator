package com.quizapp.model;

import jakarta.persistence.*;

import java.time.LocalDate;

/**
 * A league player's final daily-quiz score for one day - kept for good, so a league's history never
 * disappears (the quiz itself is deleted after 7 days and DailyQuizResult after 90).
 *
 * Only written for a quiz handed in ON its own day (the league deadline: answer that day or score 0), and
 * only for players who are in a league, plus the Expert (the benchmark opponent). Written when the attempt
 * is graded and refreshed if an admin changes a decision later - see LeagueScoreService.
 */
@Entity
@Table(name = "league_day_scores",
        uniqueConstraints = @UniqueConstraint(name = "uq_league_day_scores_user_day", columnNames = {"user_id", "quiz_date"}))
public class LeagueDayScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "quiz_date", nullable = false)
    private LocalDate quizDate;

    @Column(nullable = false)
    private int score;

    @Column(name = "max_score", nullable = false)
    private int maxScore;

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public LocalDate getQuizDate() { return quizDate; }
    public void setQuizDate(LocalDate quizDate) { this.quizDate = quizDate; }
    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }
    public int getMaxScore() { return maxScore; }
    public void setMaxScore(int maxScore) { this.maxScore = maxScore; }
}
