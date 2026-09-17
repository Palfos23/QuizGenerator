package com.quizapp.dto;

public class RoomParticipantDto {
    private Long id;
    private String displayName;
    private String color;
    private boolean connected;
    private boolean isHost;

    public RoomParticipantDto(Long id, String displayName, String color, boolean connected, boolean isHost) {
        this.id = id;
        this.displayName = displayName;
        this.color = color;
        this.connected = connected;
        this.isHost = isHost;
    }

    public Long getId() { return id; }
    public String getDisplayName() { return displayName; }
    public String getColor() { return color; }
    public boolean isConnected() { return connected; }
    public boolean isHost() { return isHost; }
}
