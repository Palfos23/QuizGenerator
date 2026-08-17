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

    // columnDefinition is set explicitly so Hibernate creates a plain varchar
    // column instead of its default (a CHECK constraint baked from whatever
    // RoomGameType constants exist at the moment this table is first
    // created). ddl-auto=update never widens an existing CHECK constraint
    // when a new game type is added later - that bit it once already (see
    // the migration note for the 2026-08 fix required on the existing
    // production table). A plain varchar column sidesteps the problem for
    // every environment created from here on.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "varchar(40)")
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
