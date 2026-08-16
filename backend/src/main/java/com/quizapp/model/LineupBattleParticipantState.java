package com.quizapp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

// Mirrors GridBattleParticipantState exactly.
@Entity
@Table(name = "lineup_battle_participant_states")
public class LineupBattleParticipantState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_state_id", nullable = false)
    @JsonIgnore
    private LineupBattleRoomState roomState;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "participant_id", nullable = false)
    private GameRoomParticipant participant;

    // Resets to 0 whenever a new lineup starts.
    @Column(nullable = false)
    private int livesUsedThisLineup = 0;

    @Column(nullable = false)
    private int totalScore = 0;

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

    public GameRoomParticipant getParticipant() {
        return participant;
    }

    public void setParticipant(GameRoomParticipant participant) {
        this.participant = participant;
    }

    public int getLivesUsedThisLineup() {
        return livesUsedThisLineup;
    }

    public void setLivesUsedThisLineup(int livesUsedThisLineup) {
        this.livesUsedThisLineup = livesUsedThisLineup;
    }

    public int getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(int totalScore) {
        this.totalScore = totalScore;
    }
}
