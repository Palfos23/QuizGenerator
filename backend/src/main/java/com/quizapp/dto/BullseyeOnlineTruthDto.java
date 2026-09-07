package com.quizapp.dto;

// The real best-possible answer(s) for the round (not just what players
// guessed), annotated with who found it (if anyone) - the online equivalent
// of BullseyeGame.vue's bullseyeAnswersWithFoundState, computed server-side
// here since (unlike pass-and-play) the client doesn't already have the
// answer key memoized.
public class BullseyeOnlineTruthDto {
    private Long athleteId;
    private String athleteName;
    private int statValue;
    private String foundBy;

    public BullseyeOnlineTruthDto(Long athleteId, String athleteName, int statValue, String foundBy) {
        this.athleteId = athleteId;
        this.athleteName = athleteName;
        this.statValue = statValue;
        this.foundBy = foundBy;
    }

    public Long getAthleteId() { return athleteId; }
    public String getAthleteName() { return athleteName; }
    public int getStatValue() { return statValue; }
    public String getFoundBy() { return foundBy; }
}
