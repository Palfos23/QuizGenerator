package com.quizapp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "flashback_room_states")
public class FlashbackRoomState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false, unique = true)
    @JsonIgnore
    private GameRoom room;

    // Host-chosen round count, picked at create time - same shape as
    // TensionRoomState.questionIds (unlike Bullseye, Flashback's round count
    // doesn't depend on the final headcount).
    @ElementCollection
    @CollectionTable(name = "flashback_room_year_sequence", joinColumns = @JoinColumn(name = "state_id"))
    @Column(name = "year_id")
    @OrderColumn(name = "seq_order")
    private List<Long> yearIds = new ArrayList<>();

    @Column(nullable = false)
    private int currentRoundIndex = 0;

    // Which of the current round's hints is visible so far - never resets
    // independently of a round change; advances within getState itself once
    // every participant's guessed on it without anyone landing exactly (see
    // FlashbackOnlineService), same silent-advance behavior
    // FlashbackGame.vue's pass-and-play advanceTurn already has.
    @Column(nullable = false)
    private int currentHintIndex = 0;

    // Index into the room's full participant list (never shrinks - nobody's
    // ever eliminated in Flashback) - continuous across the whole round,
    // exactly like FlashbackGame.vue's currentTurnIdx: does NOT reset when a
    // new hint reveals, only between rounds.
    @Column(nullable = false)
    private int currentTurnParticipantIndex = 0;

    @Column(nullable = false)
    private boolean finished = false;

    // Guards against double-awarding a round's points if it stays "revealed"
    // across multiple polls before the host advances - same reasoning as
    // TensionRoomState.roundScored.
    @Column(nullable = false)
    private boolean roundResolved = false;

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

    public List<Long> getYearIds() {
        return yearIds;
    }

    public void setYearIds(List<Long> yearIds) {
        this.yearIds = yearIds;
    }

    public int getCurrentRoundIndex() {
        return currentRoundIndex;
    }

    public void setCurrentRoundIndex(int currentRoundIndex) {
        this.currentRoundIndex = currentRoundIndex;
    }

    public int getCurrentHintIndex() {
        return currentHintIndex;
    }

    public void setCurrentHintIndex(int currentHintIndex) {
        this.currentHintIndex = currentHintIndex;
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

    public boolean isRoundResolved() {
        return roundResolved;
    }

    public void setRoundResolved(boolean roundResolved) {
        this.roundResolved = roundResolved;
    }
}
