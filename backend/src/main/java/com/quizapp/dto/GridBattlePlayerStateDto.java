package com.quizapp.dto;

public class GridBattlePlayerStateDto {
    private Long participantId;
    private String name;
    private String color;
    private boolean connected;
    private int livesUsed;
    private boolean eliminatedThisGrid;
    private int totalScore;

    public GridBattlePlayerStateDto(Long participantId, String name, String color, boolean connected,
                                     int livesUsed, boolean eliminatedThisGrid, int totalScore) {
        this.participantId = participantId;
        this.name = name;
        this.color = color;
        this.connected = connected;
        this.livesUsed = livesUsed;
        this.eliminatedThisGrid = eliminatedThisGrid;
        this.totalScore = totalScore;
    }

    public Long getParticipantId() { return participantId; }
    public String getName() { return name; }
    public String getColor() { return color; }
    public boolean isConnected() { return connected; }
    public int getLivesUsed() { return livesUsed; }
    public boolean isEliminatedThisGrid() { return eliminatedThisGrid; }
    public int getTotalScore() { return totalScore; }
}
