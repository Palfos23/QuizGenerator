package com.quizapp.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

// Stateless guess check, same pattern as MultiplayerGuessRequest - the
// client (solo or local pass-and-play, neither of which has a single
// logged-in "attempt" to persist against) reports which slots are already
// revealed on every guess.
public class LineupGuessRequest {

    @NotNull
    private Long athleteId;

    private List<Long> revealedEntryIds;

    public Long getAthleteId() { return athleteId; }
    public void setAthleteId(Long athleteId) { this.athleteId = athleteId; }
    public List<Long> getRevealedEntryIds() { return revealedEntryIds; }
    public void setRevealedEntryIds(List<Long> revealedEntryIds) { this.revealedEntryIds = revealedEntryIds; }
}
