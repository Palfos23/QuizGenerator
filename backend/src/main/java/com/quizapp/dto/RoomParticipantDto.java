package com.quizapp.dto;

public class RoomParticipantDto {
    private Long id;
    private String displayName;
    private String color;
    private boolean connected;

    public RoomParticipantDto(Long id, String displayName, String color, boolean connected) {
        this.id = id;
        this.displayName = displayName;
        this.color = color;
        this.connected = connected;
    }

    public Long getId() { return id; }
    public String getDisplayName() { return displayName; }
    public String getColor() { return color; }
    public boolean isConnected() { return connected; }
}
