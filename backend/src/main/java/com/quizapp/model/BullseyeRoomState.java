package com.quizapp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "bullseye_room_states")
public class BullseyeRoomState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false, unique = true)
    @JsonIgnore
    private GameRoom room;

    // One elimination per round means exactly (starting player count - 1)
    // rounds - the whole sequence is picked at once, but only once the room
    // actually starts (see BullseyeOnlineService#startGame), since the count
    // depends on the final roster, unlike every other online game's sequence
    // (picked at create time, from a host-chosen count).
    @ElementCollection
    @CollectionTable(name = "bullseye_room_question_sequence", joinColumns = @JoinColumn(name = "state_id"))
    @Column(name = "question_id")
    @OrderColumn(name = "seq_order")
    private List<Long> questionIds = new ArrayList<>();

    @Column(nullable = false)
    private int currentQuestionIndex = 0;

    // Index into the CURRENT round's active (non-eliminated) participant list,
    // not the room's full participant list - unlike Tension/Grid Battle, that
    // list shrinks by one every round as someone's eliminated.
    @Column(nullable = false)
    private int currentTurnParticipantIndex = 0;

    @Column(nullable = false)
    private boolean finished = false;

    // Guards against double-applying a round's elimination if the round stays
    // "complete" across multiple polls before the host advances - same
    // reasoning as TensionRoomState.roundScored.
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

    public List<Long> getQuestionIds() {
        return questionIds;
    }

    public void setQuestionIds(List<Long> questionIds) {
        this.questionIds = questionIds;
    }

    public int getCurrentQuestionIndex() {
        return currentQuestionIndex;
    }

    public void setCurrentQuestionIndex(int currentQuestionIndex) {
        this.currentQuestionIndex = currentQuestionIndex;
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
