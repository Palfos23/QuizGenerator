package com.quizapp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "bullseye_round_answers")
public class BullseyeRoundAnswer {

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

    // The raw name text this player picked - matched server-side against the
    // question's real entries when the round resolves (see
    // BullseyeOnlineService#resolveRound), same "store the guess, judge it
    // later" shape as TensionRoundAnswer.
    @Column(nullable = false, length = 255)
    private String guessedName;

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

    public String getGuessedName() {
        return guessedName;
    }

    public void setGuessedName(String guessedName) {
        this.guessedName = guessedName;
    }
}
