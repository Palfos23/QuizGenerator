package com.quizapp.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

// One "Flashback" round's content: a secret year plus up to 5 admin-authored
// clues about it, ordered hardest to easiest. Sent to players in full the
// moment a round is chosen (see FlashbackYearDto) - same trust model as
// BullseyeQuestion/TensionQuestion (no secrecy enforced server-side beyond
// that point), not GridEntry's withhold-until-solved approach, since there's
// no persisted per-player attempt to check a guess against here either.
@Entity
@Table(name = "flashback_years")
public class FlashbackYear {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Admin-internal label - never shown to players.
    @NotBlank
    @Column(nullable = false)
    private String title;

    // Free text, e.g. "Sport", "Music" - used to filter which years a game
    // draws from. Deliberately not GridCategory-backed (unlike Bullseye's
    // "sport") - a year isn't tied to an Athlete/Subject at all, so this
    // mirrors TensionQuestion.mainCategory instead.
    @Column(name = "category")
    private String category;

    // The answer players are trying to guess exactly - never sent to players
    // until a round starts (see class comment on why that's fine).
    // Explicit column name - "year" is an H2 reserved word (same class of
    // issue as FiveOhOneEntry.value), so the unquoted default would break
    // table creation on the H2 dev profile; Postgres (prod) has no such issue,
    // but there's no reason to rely on that difference when a rename avoids
    // it outright for everyone.
    @NotNull
    @Column(name = "target_year", nullable = false)
    private Integer year;

    // Ordered hardest -> easiest, at most 5 - validated in
    // FlashbackAdminService, not here, so a partial/invalid save fails with a
    // clear message rather than a DB constraint error.
    @ElementCollection
    @CollectionTable(name = "flashback_hints", joinColumns = @JoinColumn(name = "flashback_year_id"))
    @OrderColumn(name = "hint_order")
    @Column(name = "hint_text", length = 500)
    private List<String> hints = new ArrayList<>();

    // Same purpose as Grid.excludedFromGridBattle - retires a stale/wrong year
    // from the random/manual pick pool without deleting it.
    @Column(name = "excluded_from_flashback", nullable = false, columnDefinition = "boolean default false")
    private boolean excludedFromFlashback = false;

    // Same purpose as Grid.updatedAt - see that field for the full reasoning.
    @Column(name = "updated_at")
    private Instant updatedAt;

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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public List<String> getHints() {
        return hints;
    }

    public void setHints(List<String> hints) {
        this.hints.clear();
        if (hints != null) {
            this.hints.addAll(hints);
        }
    }

    public boolean isExcludedFromFlashback() {
        return excludedFromFlashback;
    }

    public void setExcludedFromFlashback(boolean excludedFromFlashback) {
        this.excludedFromFlashback = excludedFromFlashback;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
