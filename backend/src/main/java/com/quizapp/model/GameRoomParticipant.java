package com.quizapp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;

@Entity
@Table(name = "game_room_participants")
public class GameRoomParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    @JsonIgnore
    private GameRoom room;

    @NotBlank
    @Column(nullable = false)
    private String userEmail;

    @NotBlank
    @Column(nullable = false)
    private String displayName;

    @Column(nullable = false)
    private String color;

    @Column(nullable = false)
    private int joinOrder;

    // Updated on every poll from this participant - if it's too old, the frontend
    // treats them as disconnected. No separate heartbeat endpoint needed since
    // polling for game state doubles as the heartbeat.
    @Column(nullable = false)
    private Instant lastSeenAt = Instant.now();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public GameRoom getRoom() {
        return room;
    }

    public void setRoom(GameRoom room) {
        this.room = room;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public int getJoinOrder() {
        return joinOrder;
    }

    public void setJoinOrder(int joinOrder) {
        this.joinOrder = joinOrder;
    }

    public Instant getLastSeenAt() {
        return lastSeenAt;
    }

    public void setLastSeenAt(Instant lastSeenAt) {
        this.lastSeenAt = lastSeenAt;
    }
}
