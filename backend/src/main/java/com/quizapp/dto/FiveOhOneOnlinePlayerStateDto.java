package com.quizapp.dto;

public class FiveOhOneOnlinePlayerStateDto {
    private Long participantId;
    private String name;
    private String color;
    private boolean connected;
    private int total;

    public FiveOhOneOnlinePlayerStateDto(Long participantId, String name, String color, boolean connected, int total) {
        this.participantId = participantId;
        this.name = name;
        this.color = color;
        this.connected = connected;
        this.total = total;
    }

    public Long getParticipantId() { return participantId; }
    public String getName() { return name; }
    public String getColor() { return color; }
    public boolean isConnected() { return connected; }
    public int getTotal() { return total; }
}
