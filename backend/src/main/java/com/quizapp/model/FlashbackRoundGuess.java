package com.quizapp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "flashback_round_guesses")
public class FlashbackRoundGuess {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_state_id", nullable = false)
    @JsonIgnore
    private FlashbackRoomState roomState;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "participant_id", nullable = false)
    private GameRoomParticipant participant;

    @Column(nullable = false)
    private int guessedYear;

    // Which hint was showing when this guess was made - all of a round's
    // guesses stay in this same table across every hint (cleared together
    // when the round ends), same as FlashbackGame.vue's pass-and-play
    // roundGuesses list, so the "guessed years per clue" recap can group by it.
    @Column(nullable = false)
    private int hintIndex;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public FlashbackRoomState getRoomState() {
        return roomState;
    }

    public void setRoomState(FlashbackRoomState roomState) {
        this.roomState = roomState;
    }

    public GameRoomParticipant getParticipant() {
        return participant;
    }

    public void setParticipant(GameRoomParticipant participant) {
        this.participant = participant;
    }

    public int getGuessedYear() {
        return guessedYear;
    }

    public void setGuessedYear(int guessedYear) {
        this.guessedYear = guessedYear;
    }

    public int getHintIndex() {
        return hintIndex;
    }

    public void setHintIndex(int hintIndex) {
        this.hintIndex = hintIndex;
    }
}
