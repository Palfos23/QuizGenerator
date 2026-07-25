package com.quizapp.dto;

public class TensionOnlinePlayerDto {
    private Long participantId;
    private String name;
    private String color;
    private boolean connected;
    private boolean answered;
    private int totalScore;

    public TensionOnlinePlayerDto(Long participantId, String name, String color, boolean connected,
                                   boolean answered, int totalScore) {
        this.participantId = participantId;
        this.name = name;
        this.color = color;
        this.connected = connected;
        this.answered = answered;
        this.totalScore = totalScore;
    }

    public Long getParticipantId() { return participantId; }
    public String getName() { return name; }
    public String getColor() { return color; }
    public boolean isConnected() { return connected; }
    public boolean isAnswered() { return answered; }
    public int getTotalScore() { return totalScore; }
}
