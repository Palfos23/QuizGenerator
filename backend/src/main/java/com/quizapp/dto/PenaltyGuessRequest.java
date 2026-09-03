package com.quizapp.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

// Stateless guess check, same pattern as LineupGuessRequest - solo and local
// pass-and-play both report which kicks are already revealed on every guess,
// since there's no persisted per-user attempt to check against.
public class PenaltyGuessRequest {

    @NotNull
    private Long athleteId;

    private List<Long> revealedKickIds;

    public Long getAthleteId() { return athleteId; }
    public void setAthleteId(Long athleteId) { this.athleteId = athleteId; }
    public List<Long> getRevealedKickIds() { return revealedKickIds; }
    public void setRevealedKickIds(List<Long> revealedKickIds) { this.revealedKickIds = revealedKickIds; }
}
