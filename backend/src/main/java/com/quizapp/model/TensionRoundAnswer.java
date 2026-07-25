package com.quizapp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "tension_round_answers")
public class TensionRoundAnswer {

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

    @Column(nullable = false, length = 500)
    private String answerText;

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

    public String getAnswerText() {
        return answerText;
    }

    public void setAnswerText(String answerText) {
        this.answerText = answerText;
    }
}
