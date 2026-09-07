package com.quizapp.dto;

public class BullseyeOnlinePlayerDto {
    private Long participantId;
    private String name;
    private String color;
    private boolean connected;
    private boolean answered;
    private boolean eliminated;
    private Integer eliminatedAtRound;

    public BullseyeOnlinePlayerDto(Long participantId, String name, String color, boolean connected,
                                    boolean answered, boolean eliminated, Integer eliminatedAtRound) {
        this.participantId = participantId;
        this.name = name;
        this.color = color;
        this.connected = connected;
        this.answered = answered;
        this.eliminated = eliminated;
        this.eliminatedAtRound = eliminatedAtRound;
    }

    public Long getParticipantId() { return participantId; }
    public String getName() { return name; }
    public String getColor() { return color; }
    public boolean isConnected() { return connected; }
    public boolean isAnswered() { return answered; }
    public boolean isEliminated() { return eliminated; }
    public Integer getEliminatedAtRound() { return eliminatedAtRound; }
}
