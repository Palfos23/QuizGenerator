package com.quizapp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

// Mirrors GridBattleSolvedEntry exactly.
@Entity
@Table(name = "lineup_battle_solved_entries")
public class LineupBattleSolvedEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_state_id", nullable = false)
    @JsonIgnore
    private LineupBattleRoomState roomState;

    @Column(nullable = false)
    private Long lineupEntryId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "solved_by_participant_id", nullable = false)
    private GameRoomParticipant solvedBy;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LineupBattleRoomState getRoomState() {
        return roomState;
    }

    public void setRoomState(LineupBattleRoomState roomState) {
        this.roomState = roomState;
    }

    public Long getLineupEntryId() {
        return lineupEntryId;
    }

    public void setLineupEntryId(Long lineupEntryId) {
        this.lineupEntryId = lineupEntryId;
    }

    public GameRoomParticipant getSolvedBy() {
        return solvedBy;
    }

    public void setSolvedBy(GameRoomParticipant solvedBy) {
        this.solvedBy = solvedBy;
    }
}
