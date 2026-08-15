package com.quizapp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "imposter_flipped_tiles")
public class ImposterFlippedTile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_state_id", nullable = false)
    @JsonIgnore
    private ImposterRoomState roomState;

    @Column(nullable = false)
    private Long tileId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "flipped_by_participant_id", nullable = false)
    private GameRoomParticipant flippedBy;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ImposterRoomState getRoomState() {
        return roomState;
    }

    public void setRoomState(ImposterRoomState roomState) {
        this.roomState = roomState;
    }

    public Long getTileId() {
        return tileId;
    }

    public void setTileId(Long tileId) {
        this.tileId = tileId;
    }

    public GameRoomParticipant getFlippedBy() {
        return flippedBy;
    }

    public void setFlippedBy(GameRoomParticipant flippedBy) {
        this.flippedBy = flippedBy;
    }
}
