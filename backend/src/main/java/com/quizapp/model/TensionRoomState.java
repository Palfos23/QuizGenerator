package com.quizapp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tension_room_states")
public class TensionRoomState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false, unique = true)
    @JsonIgnore
    private GameRoom room;

    @ElementCollection
    @CollectionTable(name = "tension_room_question_sequence", joinColumns = @JoinColumn(name = "state_id"))
    @Column(name = "question_id")
    @OrderColumn(name = "seq_order")
    private List<Long> questionIds = new ArrayList<>();

    // The host's category filter from room creation, kept so a mid-game question
    // swap (TensionOnlineService#rerollQuestion) draws from the same pool the host
    // originally picked instead of the whole bank. Both nullable/empty-by-default -
    // rooms created before this existed simply have no filter stored.
    @Column(name = "category_filter")
    private String categoryFilter;

    @ElementCollection
    @CollectionTable(name = "tension_room_excluded_categories", joinColumns = @JoinColumn(name = "state_id"))
    @Column(name = "category_name")
    private List<String> excludedCategories = new ArrayList<>();

    public String getCategoryFilter() {
        return categoryFilter;
    }

    public void setCategoryFilter(String categoryFilter) {
        this.categoryFilter = categoryFilter;
    }

    public List<String> getExcludedCategories() {
        return excludedCategories;
    }

    public void setExcludedCategories(List<String> excludedCategories) {
        this.excludedCategories = excludedCategories;
    }

    @Column(nullable = false)
    private int currentQuestionIndex = 0;

    @Column(nullable = false)
    private int currentTurnParticipantIndex = 0;

    public int getCurrentTurnParticipantIndex() {
        return currentTurnParticipantIndex;
    }

    public void setCurrentTurnParticipantIndex(int currentTurnParticipantIndex) {
        this.currentTurnParticipantIndex = currentTurnParticipantIndex;
    }

    @Column(nullable = false)
    private boolean finished = false;

    // Guards against double-applying a round's scores to cumulative totals if the
    // round stays "complete" across multiple polls before anyone advances.
    @Column(nullable = false)
    private boolean roundScored = false;

    public boolean isRoundScored() {
        return roundScored;
    }

    public void setRoundScored(boolean roundScored) {
        this.roundScored = roundScored;
    }

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

    public boolean isFinished() {
        return finished;
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
    }
}
