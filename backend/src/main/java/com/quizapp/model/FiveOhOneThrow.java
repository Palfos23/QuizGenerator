package com.quizapp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "five_oh_one_throws")
public class FiveOhOneThrow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_state_id", nullable = false)
    @JsonIgnore
    private FiveOhOneRoomState roomState;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "thrown_by_participant_id", nullable = false)
    private GameRoomParticipant thrownBy;

    @Column(nullable = false)
    private Long entryId;

    @Column(nullable = false)
    private String entryName;

    @Column(nullable = false)
    private int rawValue;

    @Column(nullable = false)
    private int score;

    @Column(nullable = false)
    private boolean bust;

    @Column(nullable = false)
    private int resultingTotal;

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

    public GameRoomParticipant getThrownBy() {
        return thrownBy;
    }

    public void setThrownBy(GameRoomParticipant thrownBy) {
        this.thrownBy = thrownBy;
    }

    public Long getEntryId() {
        return entryId;
    }

    public void setEntryId(Long entryId) {
        this.entryId = entryId;
    }

    public String getEntryName() {
        return entryName;
    }

    public void setEntryName(String entryName) {
        this.entryName = entryName;
    }

    public int getRawValue() {
        return rawValue;
    }

    public void setRawValue(int rawValue) {
        this.rawValue = rawValue;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public boolean isBust() {
        return bust;
    }

    public void setBust(boolean bust) {
        this.bust = bust;
    }

    public int getResultingTotal() {
        return resultingTotal;
    }

    public void setResultingTotal(int resultingTotal) {
        this.resultingTotal = resultingTotal;
    }
}
