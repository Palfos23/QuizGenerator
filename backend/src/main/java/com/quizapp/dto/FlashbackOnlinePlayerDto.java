package com.quizapp.dto;

public class FlashbackOnlinePlayerDto {
    private Long participantId;
    private String name;
    private String color;
    private boolean connected;
    private int totalScore;
    // Everyone can guess whenever they like (no turn order) - this is what
    // the client uses instead to show "waiting on the rest of the table" for
    // whoever's already submitted a guess on the current hint.
    private boolean hasAnsweredCurrentHint;

    public FlashbackOnlinePlayerDto(Long participantId, String name, String color, boolean connected, int totalScore,
                                     boolean hasAnsweredCurrentHint) {
        this.participantId = participantId;
        this.name = name;
        this.color = color;
        this.connected = connected;
        this.totalScore = totalScore;
        this.hasAnsweredCurrentHint = hasAnsweredCurrentHint;
    }

    public Long getParticipantId() { return participantId; }
    public String getName() { return name; }
    public String getColor() { return color; }
    public boolean isConnected() { return connected; }
    public int getTotalScore() { return totalScore; }
    public boolean isHasAnsweredCurrentHint() { return hasAnsweredCurrentHint; }
}
