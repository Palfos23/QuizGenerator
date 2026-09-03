package com.quizapp.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

// A single "guess who took each penalty" board for one real shootout. Modeled
// as a close sibling of Lineup (guess-the-athlete, candidate/entry split) but
// deliberately NOT weekly-scheduled - unlike Grid/Lineup, there's no solo
// "this week's board" concept here. A shootout is just pool-based, the same
// way Bullseye/Imposter/501 boards are: admin adds as many as they like,
// players (solo or pass-and-play) pick one to play from the pool. See
// PenaltyShootoutPlayService for how the pool is drawn from.
@Entity
@Table(name = "penalty_shootouts")
public class PenaltyShootout {

    // Every shootout is football-only by construction, same reasoning as
    // Lineup.CATEGORY (which this intentionally reuses rather than duplicating -
    // see PenaltyShootoutPlayService.searchCandidates).
    public static final String CATEGORY = Lineup.CATEGORY;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // e.g. "2006 World Cup Final"
    @NotBlank
    @Column(nullable = false)
    private String title;

    // Longer free-text context, e.g. "Italy win 5-3 on penalties after a 1-1
    // draw." Optional, shown under the title the same way a Lineup's
    // competition field is.
    @Column(length = 1000)
    private String competition;

    // The historical date the match itself was played. Optional context only.
    private LocalDate matchDate;

    @NotBlank
    @Column(nullable = false)
    private String teamName;

    private String teamCrestUrl;

    @NotBlank
    @Column(nullable = false)
    private String opponentName;

    private String opponentCrestUrl;

    // How many kicks either side actually scored in real life, shown as
    // context (e.g. "5-3") - separate from maxStrikes below, which governs
    // this board's own guessing game, not the real match result.
    private Integer teamPensScored;
    private Integer opponentPensScored;

    @Column(name = "max_strikes", nullable = false)
    private int maxStrikes = 5;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    // The full searchable pool for the guess box - every kicker plus any
    // decoys (e.g. a player who was subbed off before the shootout). Mirrors
    // LineupCandidate/Lineup.candidates exactly.
    @OneToMany(mappedBy = "shootout", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<PenaltyCandidate> candidates = new HashSet<>();

    // The actual kicks, in shootout order.
    @OneToMany(mappedBy = "shootout", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private Set<PenaltyKick> kicks = new HashSet<>();

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

    public Integer getTeamPensScored() {
        return teamPensScored;
    }

    public void setTeamPensScored(Integer teamPensScored) {
        this.teamPensScored = teamPensScored;
    }

    public Integer getOpponentPensScored() {
        return opponentPensScored;
    }

    public void setOpponentPensScored(Integer opponentPensScored) {
        this.opponentPensScored = opponentPensScored;
    }

    public int getMaxStrikes() {
        return maxStrikes;
    }

    public void setMaxStrikes(int maxStrikes) {
        this.maxStrikes = maxStrikes;
    }

    public Set<PenaltyCandidate> getCandidates() {
        return candidates;
    }

    public void setCandidates(Set<PenaltyCandidate> candidates) {
        Set<PenaltyCandidate> incoming = candidates != null ? candidates : new HashSet<>();
        this.candidates.removeIf(existing -> !incoming.contains(existing));
        for (PenaltyCandidate c : incoming) {
            if (!this.candidates.contains(c)) {
                c.setShootout(this);
                this.candidates.add(c);
            }
        }
    }

    public Set<PenaltyKick> getKicks() {
        return kicks;
    }

    public void setKicks(Set<PenaltyKick> kicks) {
        Set<PenaltyKick> incoming = kicks != null ? kicks : new HashSet<>();
        this.kicks.removeIf(existing -> !incoming.contains(existing));
        for (PenaltyKick k : incoming) {
            if (!this.kicks.contains(k)) {
                k.setShootout(this);
                this.kicks.add(k);
            }
        }
    }
}
