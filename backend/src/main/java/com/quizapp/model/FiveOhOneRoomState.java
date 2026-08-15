package com.quizapp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "five_oh_one_room_states")
public class FiveOhOneRoomState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false, unique = true)
    @JsonIgnore
    private GameRoom room;

    @Column(nullable = false)
    private Long categoryId;

    @Column(nullable = false)
    private int currentTurnParticipantIndex = 0;

    // Set the instant either player first lands in the -10..0 checkout window -
    // mirrors FiveOhOneGame.vue's windowReacher client state exactly.
    @Column
    private Long windowReacherParticipantId;

    @Column
    private Long winnerParticipantId;

    @Column(nullable = false)
    private boolean finished = false;

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

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public int getCurrentTurnParticipantIndex() {
        return currentTurnParticipantIndex;
    }

    public void setCurrentTurnParticipantIndex(int currentTurnParticipantIndex) {
        this.currentTurnParticipantIndex = currentTurnParticipantIndex;
    }

    public Long getWindowReacherParticipantId() {
        return windowReacherParticipantId;
    }

    public void setWindowReacherParticipantId(Long windowReacherParticipantId) {
        this.windowReacherParticipantId = windowReacherParticipantId;
    }

    public Long getWinnerParticipantId() {
        return winnerParticipantId;
    }

    public void setWinnerParticipantId(Long winnerParticipantId) {
        this.winnerParticipantId = winnerParticipantId;
    }

    public boolean isFinished() {
        return finished;
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
    }
}
