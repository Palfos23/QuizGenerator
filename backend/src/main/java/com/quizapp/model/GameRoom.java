package com.quizapp.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "game_rooms")
public class GameRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, unique = true, length = 8)
    private String roomCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoomGameType gameType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoomStatus status = RoomStatus.WAITING;

    @Column(nullable = false)
    private String hostEmail;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("joinOrder ASC")
    private List<GameRoomParticipant> participants = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRoomCode() {
        return roomCode;
    }

    public void setRoomCode(String roomCode) {
        this.roomCode = roomCode;
    }

    public RoomGameType getGameType() {
        return gameType;
    }

    public void setGameType(RoomGameType gameType) {
        this.gameType = gameType;
    }

    public RoomStatus getStatus() {
        return status;
    }

    public void setStatus(RoomStatus status) {
        this.status = status;
    }

    public String getHostEmail() {
        return hostEmail;
    }

    public void setHostEmail(String hostEmail) {
        this.hostEmail = hostEmail;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public List<GameRoomParticipant> getParticipants() {
        return participants;
    }
}
