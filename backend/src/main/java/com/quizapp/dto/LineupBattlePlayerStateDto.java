package com.quizapp.dto;

public class LineupBattlePlayerStateDto {
    private Long participantId;
    private String name;
    private String color;
    private boolean connected;
    private int livesUsed;
    private boolean eliminatedThisLineup;
    private int totalScore;

    public LineupBattlePlayerStateDto(Long participantId, String name, String color, boolean connected,
                                       int livesUsed, boolean eliminatedThisLineup, int totalScore) {
        this.participantId = participantId;
        this.name = name;
        this.color = color;
        this.connected = connected;
        this.livesUsed = livesUsed;
        this.eliminatedThisLineup = eliminatedThisLineup;
        this.totalScore = totalScore;
    }

    public Long getParticipantId() { return participantId; }
    public String getName() { return name; }
    public String getColor() { return color; }
    public boolean isConnected() { return connected; }
    public int getLivesUsed() { return livesUsed; }
    public boolean isEliminatedThisLineup() { return eliminatedThisLineup; }
    public int getTotalScore() { return totalScore; }
}
