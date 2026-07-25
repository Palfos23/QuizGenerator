package com.quizapp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "grid_battle_room_states")
public class GridBattleRoomState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false, unique = true)
    @JsonIgnore
    private GameRoom room;

    // The ordered sequence of grid ids for this session (2-4 of them).
    @ElementCollection
    @CollectionTable(name = "grid_battle_grid_sequence", joinColumns = @JoinColumn(name = "state_id"))
    @Column(name = "grid_id")
    @OrderColumn(name = "seq_order")
    private List<Long> gridIds = new ArrayList<>();

    @Column(nullable = false)
    private int currentGridIndex = 0;

    @Column(nullable = false)
    private int currentTurnParticipantIndex = 0;

    @Column(nullable = false)
    private boolean finished = false;

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

    public List<Long> getGridIds() {
        return gridIds;
    }

    public void setGridIds(List<Long> gridIds) {
        this.gridIds = gridIds;
    }

    public int getCurrentGridIndex() {
        return currentGridIndex;
    }

    public void setCurrentGridIndex(int currentGridIndex) {
        this.currentGridIndex = currentGridIndex;
    }

    public int getCurrentTurnParticipantIndex() {
        return currentTurnParticipantIndex;
    }

    public void setCurrentTurnParticipantIndex(int currentTurnParticipantIndex) {
        this.currentTurnParticipantIndex = currentTurnParticipantIndex;
    }

    public boolean isFinished() {
        return finished;
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
    }
}
