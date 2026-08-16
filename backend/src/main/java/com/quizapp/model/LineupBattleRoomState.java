package com.quizapp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

// Mirrors GridBattleRoomState exactly - see that class for the reasoning.
// A Starting XI Battle session steps through a sequence of 2-4 lineup boards
// the same way Grid Battle steps through 2-4 grids.
@Entity
@Table(name = "lineup_battle_room_states")
public class LineupBattleRoomState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false, unique = true)
    @JsonIgnore
    private GameRoom room;

    @ElementCollection
    @CollectionTable(name = "lineup_battle_lineup_sequence", joinColumns = @JoinColumn(name = "state_id"))
    @Column(name = "lineup_id")
    @OrderColumn(name = "seq_order")
    private List<Long> lineupIds = new ArrayList<>();

    @Column(nullable = false)
    private int currentLineupIndex = 0;

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

    public List<Long> getLineupIds() {
        return lineupIds;
    }

    public void setLineupIds(List<Long> lineupIds) {
        this.lineupIds = lineupIds;
    }

    public int getCurrentLineupIndex() {
        return currentLineupIndex;
    }

    public void setCurrentLineupIndex(int currentLineupIndex) {
        this.currentLineupIndex = currentLineupIndex;
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
