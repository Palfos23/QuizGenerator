package com.quizapp.model;

import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;

/**
 * One competition inside a league, from startDate to endDate (both inclusive, quiz days). A league runs one
 * season at a time; when it is over the owner can start the next one, and every finished season keeps its
 * table, matchups and recap.
 *
 * Head-to-head: `regularWeeks` weeks of daily matchups, then - when `playoffs` is on and at least four
 * players take part - a semi-final day and a final day. The matchups are drawn on the season's first day
 * (`scheduled`), from the members at that moment; until then anyone can still join.
 * Points: no matchups; endDate null means the season never ends (like the global league).
 */
@Entity
@Table(name = "league_seasons", uniqueConstraints = @UniqueConstraint(columnNames = {"league_id", "number"}))
public class LeagueSeason {

    public static final int PLAYOFF_SIZE = 4;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "league_id")
    private League league;

    @Column(nullable = false)
    private int number;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LeagueFormat format;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    // Null only for an open-ended points season.
    @Column(name = "end_date")
    private LocalDate endDate;

    // Head-to-head: weeks of regular-season matchups (the playoff days come after them). Null for points.
    @Column(name = "regular_weeks")
    private Integer regularWeeks;

    @Column(nullable = false)
    private boolean playoffs;

    // Head-to-head: the matchups have been drawn (LeagueMatch rows exist).
    @Column(nullable = false)
    private boolean scheduled;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    /** Last day of the regular season (head-to-head), i.e. the day before the semi-finals if there are playoffs. */
    public LocalDate regularEndDate() {
        if (format != LeagueFormat.HEAD_TO_HEAD || regularWeeks == null) return endDate;
        return startDate.plusDays(regularWeeks * 7L - 1);
    }

    /** Sets endDate from the start, the regular weeks and whether there are playoffs (two extra days). */
    public void recalculateEndDate() {
        if (format == LeagueFormat.HEAD_TO_HEAD && regularWeeks != null) {
            endDate = regularEndDate().plusDays(playoffs ? 2 : 0);
        }
    }

    public Long getId() { return id; }
    public League getLeague() { return league; }
    public void setLeague(League league) { this.league = league; }
    public int getNumber() { return number; }
    public void setNumber(int number) { this.number = number; }
    public LeagueFormat getFormat() { return format; }
    public void setFormat(LeagueFormat format) { this.format = format; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public Integer getRegularWeeks() { return regularWeeks; }
    public void setRegularWeeks(Integer regularWeeks) { this.regularWeeks = regularWeeks; }
    public boolean isPlayoffs() { return playoffs; }
    public void setPlayoffs(boolean playoffs) { this.playoffs = playoffs; }
    public boolean isScheduled() { return scheduled; }
    public void setScheduled(boolean scheduled) { this.scheduled = scheduled; }
    public Instant getCreatedAt() { return createdAt; }
}
