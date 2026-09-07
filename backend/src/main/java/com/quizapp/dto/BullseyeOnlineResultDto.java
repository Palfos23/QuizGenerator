package com.quizapp.dto;

// One player's resolved guess, once a round's revealed - closest-first is left
// to the frontend to sort/paginate for the reveal animation, same as the
// pass-and-play component already does client-side.
public class BullseyeOnlineResultDto {
    private Long participantId;
    private String name;
    private String guessedName;
    private int statValue;
    private int distance;
    private boolean eliminatedThisRound;

    public BullseyeOnlineResultDto(Long participantId, String name, String guessedName, int statValue,
                                    int distance, boolean eliminatedThisRound) {
        this.participantId = participantId;
        this.name = name;
        this.guessedName = guessedName;
        this.statValue = statValue;
        this.distance = distance;
        this.eliminatedThisRound = eliminatedThisRound;
    }

    public Long getParticipantId() { return participantId; }
    public String getName() { return name; }
    public String getGuessedName() { return guessedName; }
    public int getStatValue() { return statValue; }
    public int getDistance() { return distance; }
    public boolean isEliminatedThisRound() { return eliminatedThisRound; }
}
