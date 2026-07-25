package com.quizapp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "grid_battle_solved_entries")
public class GridBattleSolvedEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_state_id", nullable = false)
    @JsonIgnore
    private GridBattleRoomState roomState;

    @Column(nullable = false)
    private Long gridEntryId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "solved_by_participant_id", nullable = false)
    private GameRoomParticipant solvedBy;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public GridBattleRoomState getRoomState() {
        return roomState;
    }

    public void setRoomState(GridBattleRoomState roomState) {
        this.roomState = roomState;
    }

    public Long getGridEntryId() {
        return gridEntryId;
    }

    public void setGridEntryId(Long gridEntryId) {
        this.gridEntryId = gridEntryId;
    }

    public GameRoomParticipant getSolvedBy() {
        return solvedBy;
    }

    public void setSolvedBy(GameRoomParticipant solvedBy) {
        this.solvedBy = solvedBy;
    }
}
