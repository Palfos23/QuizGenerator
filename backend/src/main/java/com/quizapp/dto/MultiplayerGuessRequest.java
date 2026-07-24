package com.quizapp.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public class MultiplayerGuessRequest {

    @NotNull
    private Long athleteId;

    // Entry ids already revealed so far in this live pass-and-play session - since
    // nothing here is persisted server-side (there's no single logged-in "attempt"
    // when 2-4 local players share one screen), the client tracks progress and
    // reports it back on every guess.
    private List<Long> revealedEntryIds;

    public Long getAthleteId() {
        return athleteId;
    }

    public void setAthleteId(Long athleteId) {
        this.athleteId = athleteId;
    }

    public List<Long> getRevealedEntryIds() {
        return revealedEntryIds;
    }

    public void setRevealedEntryIds(List<Long> revealedEntryIds) {
        this.revealedEntryIds = revealedEntryIds;
    }
}
