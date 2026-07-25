package com.quizapp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "tension_participant_states")
public class TensionParticipantState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_state_id", nullable = false)
    @JsonIgnore
    private TensionRoomState roomState;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "participant_id", nullable = false)
    private GameRoomParticipant participant;

    @Column(nullable = false)
    private int totalScore = 0;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public TensionRoomState getRoomState() {
        return roomState;
    }

    public void setRoomState(TensionRoomState roomState) {
        this.roomState = roomState;
    }

    public GameRoomParticipant getParticipant() {
        return participant;
    }

    public void setParticipant(GameRoomParticipant participant) {
        this.participant = participant;
    }

    public int getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(int totalScore) {
        this.totalScore = totalScore;
    }
}
