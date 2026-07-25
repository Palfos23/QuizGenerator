package com.quizapp.dto;

import com.quizapp.model.RoomGameType;
import com.quizapp.model.RoomStatus;

import java.util.List;

public class RoomDto {
    private Long id;
    private String roomCode;
    private RoomGameType gameType;
    private RoomStatus status;
    private String hostEmail;
    private List<RoomParticipantDto> participants;
    private Long yourParticipantId;
    private boolean isHost;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getRoomCode() { return roomCode; }
    public void setRoomCode(String roomCode) { this.roomCode = roomCode; }
    public RoomGameType getGameType() { return gameType; }
    public void setGameType(RoomGameType gameType) { this.gameType = gameType; }
    public RoomStatus getStatus() { return status; }
    public void setStatus(RoomStatus status) { this.status = status; }
    public String getHostEmail() { return hostEmail; }
    public void setHostEmail(String hostEmail) { this.hostEmail = hostEmail; }
    public List<RoomParticipantDto> getParticipants() { return participants; }
    public void setParticipants(List<RoomParticipantDto> participants) { this.participants = participants; }
    public Long getYourParticipantId() { return yourParticipantId; }
    public void setYourParticipantId(Long yourParticipantId) { this.yourParticipantId = yourParticipantId; }
    public boolean isHost() { return isHost; }
    public void setHost(boolean host) { isHost = host; }
}
