package com.quizapp.dto;

public class ImposterOnlinePlayerStateDto {
    private Long participantId;
    private String name;
    private String color;
    private boolean connected;
    private int totalScore;

    public ImposterOnlinePlayerStateDto(Long participantId, String name, String color, boolean connected, int totalScore) {
        this.participantId = participantId;
        this.name = name;
        this.color = color;
        this.connected = connected;
        this.totalScore = totalScore;
    }

    public Long getParticipantId() { return participantId; }
    public String getName() { return name; }
    public String getColor() { return color; }
    public boolean isConnected() { return connected; }
    public int getTotalScore() { return totalScore; }
}
