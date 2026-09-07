package com.quizapp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "bullseye_participant_states")
public class BullseyeParticipantState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_state_id", nullable = false)
    @JsonIgnore
    private BullseyeRoomState roomState;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "participant_id", nullable = false)
    private GameRoomParticipant participant;

    @Column(nullable = false)
    private boolean eliminated = false;

    // The 1-based round number (matches currentQuestionIndex + 1 at the moment
    // of elimination) this player was knocked out on - null while still active,
    // and still null for whoever's left when the game ends (the winner). Final
    // placement is this value directly (lower = eliminated earlier = placed
    // worse); the winner's placement is just the starting player count, same
    // scoring BullseyeGame.vue's pass-and-play buildFinalScores already uses.
    @Column(name = "eliminated_at_round")
    private Integer eliminatedAtRound;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public BullseyeRoomState getRoomState() {
        return roomState;
    }

    public void setRoomState(BullseyeRoomState roomState) {
        this.roomState = roomState;
    }

    public GameRoomParticipant getParticipant() {
        return participant;
    }

    public void setParticipant(GameRoomParticipant participant) {
        this.participant = participant;
    }

    public boolean isEliminated() {
        return eliminated;
    }

    public void setEliminated(boolean eliminated) {
        this.eliminated = eliminated;
    }

    public Integer getEliminatedAtRound() {
        return eliminatedAtRound;
    }

    public void setEliminatedAtRound(Integer eliminatedAtRound) {
        this.eliminatedAtRound = eliminatedAtRound;
    }
}
