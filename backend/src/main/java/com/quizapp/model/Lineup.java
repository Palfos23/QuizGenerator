package com.quizapp.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

// A single "Starting XI" board - guess every player who started a specific,
// named football match for one of the two teams. Modeled as a close sibling
// of Grid (see that class for the weekly-scheduling reasoning this borrows
// wholesale) rather than a subtype of it, since a lineup's shape - a pitch
// formation of exactly 11 shirts, not an arbitrary N-tile board - is
// different enough to not share a table cleanly.
@Entity
@Table(name = "lineups")
public class Lineup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // e.g. "Community Shield 2014"
    @NotBlank
    @Column(nullable = false)
    private String title;

    // Longer free-text context, e.g. "Arsenal's opening day XI the season after
    // winning the FA Cup." Optional, shown under the title the same way a
    // Grid's theme is.
    @Column(length = 1000)
    private String competition;

    // The historical date the match itself was played - distinct from
    // weekStartDate below, which is when this board goes live on the site.
    private LocalDate matchDate;

    // The Monday this board is "live" for, same active-window convention as
    // Grid.weekStartDate: [weekStartDate, weekStartDate + 6 days] is "this
    // week's", anything before that is archive. Unlike Grid, this is not
    // required to be unique - more than one Starting XI can go live the same
    // week.
    @NotNull
    @Column(name = "week_start_date", nullable = false)
    private LocalDate weekStartDate;

    // e.g. "4-3-3" - must be one of the shapes both admin and play UIs know how
    // to lay out (see frontend/src/services/formations.js). Determines how many
    // of the 11 entries sit in each row from goalkeeper up to attack.
    @NotBlank
    @Column(nullable = false)
    private String formation;

    // The team whose XI is being guessed.
    @NotBlank
    @Column(nullable = false)
    private String teamName;

    private String teamCrestUrl;

    // Shown as context only - never guessable.
    @NotBlank
    @Column(nullable = false)
    private String opponentName;

    private String opponentCrestUrl;

    private Integer scoreFor;
    private Integer scoreAgainst;

    @Column(name = "max_strikes", nullable = false)
    private int maxStrikes = 5;

    // Same purpose as Grid.excludedFromGridBattle - hide a stale board from
    // Starting XI Battle's random/manual pick pool without touching its
    // standalone solo playability.
    @Column(name = "excluded_from_battle", nullable = false)
    private boolean excludedFromBattle = false;

    // The full searchable pool for the guess box - the 11 correct starters plus
    // any decoys (e.g. players who were only on the bench that day). Set, not
    // List - see Grid.candidates for why.
    @OneToMany(mappedBy = "lineup", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<LineupCandidate> candidates = new HashSet<>();

    // The 11 correct starters, each pinned to a formation slot.
    @OneToMany(mappedBy = "lineup", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private Set<LineupEntry> entries = new HashSet<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCompetition() {
        return competition;
    }

    public void setCompetition(String competition) {
        this.competition = competition;
    }

    public LocalDate getMatchDate() {
        return matchDate;
    }

    public void setMatchDate(LocalDate matchDate) {
        this.matchDate = matchDate;
    }

    public LocalDate getWeekStartDate() {
        return weekStartDate;
    }

    public void setWeekStartDate(LocalDate weekStartDate) {
        this.weekStartDate = weekStartDate;
    }

    public String getFormation() {
        return formation;
    }

    public void setFormation(String formation) {
        this.formation = formation;
    }

    public String getTeamName() {
        return teamName;
    }

    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }

    public String getTeamCrestUrl() {
        return teamCrestUrl;
    }

    public void setTeamCrestUrl(String teamCrestUrl) {
        this.teamCrestUrl = teamCrestUrl;
    }

    public String getOpponentName() {
        return opponentName;
    }

    public void setOpponentName(String opponentName) {
        this.opponentName = opponentName;
    }

    public String getOpponentCrestUrl() {
        return opponentCrestUrl;
    }

    public void setOpponentCrestUrl(String opponentCrestUrl) {
        this.opponentCrestUrl = opponentCrestUrl;
    }

    public Integer getScoreFor() {
        return scoreFor;
    }

    public void setScoreFor(Integer scoreFor) {
        this.scoreFor = scoreFor;
    }

    public Integer getScoreAgainst() {
        return scoreAgainst;
    }

    public void setScoreAgainst(Integer scoreAgainst) {
        this.scoreAgainst = scoreAgainst;
    }

    public int getMaxStrikes() {
        return maxStrikes;
    }

    public void setMaxStrikes(int maxStrikes) {
        this.maxStrikes = maxStrikes;
    }

    public boolean isExcludedFromBattle() {
        return excludedFromBattle;
    }

    public void setExcludedFromBattle(boolean excludedFromBattle) {
        this.excludedFromBattle = excludedFromBattle;
    }

    public Set<LineupCandidate> getCandidates() {
        return candidates;
    }

    public void setCandidates(Set<LineupCandidate> candidates) {
        Set<LineupCandidate> incoming = candidates != null ? candidates : new HashSet<>();
        this.candidates.removeIf(existing -> !incoming.contains(existing));
        for (LineupCandidate c : incoming) {
            if (!this.candidates.contains(c)) {
                c.setLineup(this);
                this.candidates.add(c);
            }
        }
    }

    public Set<LineupEntry> getEntries() {
        return entries;
    }

    public void setEntries(Set<LineupEntry> entries) {
        Set<LineupEntry> incoming = entries != null ? entries : new HashSet<>();
        this.entries.removeIf(existing -> !incoming.contains(existing));
        for (LineupEntry e : incoming) {
            if (!this.entries.contains(e)) {
                e.setLineup(this);
                this.entries.add(e);
            }
        }
    }
}
