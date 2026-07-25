package com.quizapp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "grid_battle_participant_states")
public class GridBattleParticipantState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_state_id", nullable = false)
    @JsonIgnore
    private GridBattleRoomState roomState;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "participant_id", nullable = false)
    private GameRoomParticipant participant;

    // Resets to 0 whenever a new grid starts - elimination only lasts for the
    // current grid, not the rest of the session.
    @Column(nullable = false)
    private int livesUsedThisGrid = 0;

    @Column(nullable = false)
    private int totalScore = 0;

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

    public GameRoomParticipant getParticipant() {
        return participant;
    }

    public void setParticipant(GameRoomParticipant participant) {
        this.participant = participant;
    }

    public int getLivesUsedThisGrid() {
        return livesUsedThisGrid;
    }

    public void setLivesUsedThisGrid(int livesUsedThisGrid) {
        this.livesUsedThisGrid = livesUsedThisGrid;
    }

    public int getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(int totalScore) {
        this.totalScore = totalScore;
    }
}
