package com.quizapp.model;

import jakarta.persistence.*;

import java.time.LocalDate;

/**
 * One regular-season head-to-head matchup: two players' daily-quiz scores on one day, the higher wins.
 * Only the pairing is stored - the result is always worked out from the stored day scores (LeagueDayScore),
 * so a late admin decision on an answer updates the matchup by itself. userB null = against the Expert
 * (the odd one out that day, when the league has an odd number of players and an expert is configured).
 * Playoff matchups are not stored: they follow from the regular-season table (see LeagueStandings).
 */
@Entity
@Table(name = "league_matches", indexes = @Index(name = "idx_league_matches_season", columnList = "season_id"))
public class LeagueMatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "season_id")
    private LeagueSeason season;

    @Column(name = "quiz_date", nullable = false)
    private LocalDate quizDate;

    @Column(name = "user_a", nullable = false)
    private Long userA;

    @Column(name = "user_b")
    private Long userB;

    public LeagueMatch() {
    }

    public LeagueMatch(LeagueSeason season, LocalDate quizDate, Long userA, Long userB) {
        this.season = season;
        this.quizDate = quizDate;
        this.userA = userA;
        this.userB = userB;
    }

    public Long getId() { return id; }
    public LeagueSeason getSeason() { return season; }
    public LocalDate getQuizDate() { return quizDate; }
    public Long getUserA() { return userA; }
    public Long getUserB() { return userB; }
}
