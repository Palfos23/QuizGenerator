package com.quizapp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "five_oh_one_participant_states")
public class FiveOhOneParticipantState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_state_id", nullable = false)
    @JsonIgnore
    private FiveOhOneRoomState roomState;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "participant_id", nullable = false)
    private GameRoomParticipant participant;

    @Column(nullable = false)
    private int total = 501;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public FiveOhOneRoomState getRoomState() {
        return roomState;
    }

    public void setRoomState(FiveOhOneRoomState roomState) {
        this.roomState = roomState;
    }

    public GameRoomParticipant getParticipant() {
        return participant;
    }

    public void setParticipant(GameRoomParticipant participant) {
        this.participant = participant;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }
}
