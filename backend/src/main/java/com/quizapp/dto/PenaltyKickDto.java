package com.quizapp.dto;

// One kick as shown to a player - mirrors LineupSlotDto's reveal-only-
// once-solved shape for the athlete's identity, but scored/missed is sent
// always, solved or not. It's public history about a real match, not part of
// what's being guessed (that's who took the kick) - showing it up front, like
// Grid's tile hint badge, gives a bit of authentic drama ("kick 4 - missed...
// who choked?") rather than spoiling anything.
public class PenaltyKickDto {

    private Long id; // the PenaltyKick id
    private int kickOrder;
    private boolean forTeam;
    private boolean solved;
    private boolean guessedByUser; // true only if this specific kick was actually guessed correctly - not just revealed
    private boolean scored; // always sent, regardless of solved - see class comment
    private String athleteName; // null unless solved
    private String athletePhotoUrl; // null unless solved

    public PenaltyKickDto(Long id, int kickOrder, boolean forTeam, boolean solved, boolean guessedByUser,
                           boolean scored, String athleteName, String athletePhotoUrl) {
        this.id = id;
        this.kickOrder = kickOrder;
        this.forTeam = forTeam;
        this.solved = solved;
        this.guessedByUser = guessedByUser;
        this.scored = scored;
        this.athleteName = athleteName;
        this.athletePhotoUrl = athletePhotoUrl;
    }

    public Long getId() { return id; }
    public int getKickOrder() { return kickOrder; }
    public boolean isForTeam() { return forTeam; }
    public boolean isSolved() { return solved; }
    public boolean isGuessedByUser() { return guessedByUser; }
    public boolean isScored() { return scored; }
    public String getAthleteName() { return athleteName; }
    public String getAthletePhotoUrl() { return athletePhotoUrl; }
}
