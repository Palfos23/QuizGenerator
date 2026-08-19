package com.quizapp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "imposter_room_states")
public class ImposterRoomState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false, unique = true)
    @JsonIgnore
    private GameRoom room;

    // The ordered sequence of imposter grid ids for this session (2-4 of them).
    @ElementCollection
    @CollectionTable(name = "imposter_battle_grid_sequence", joinColumns = @JoinColumn(name = "state_id"))
    @Column(name = "grid_id")
    @OrderColumn(name = "seq_order")
    private List<Long> gridIds = new ArrayList<>();

    @Column(nullable = false)
    private int currentGridIndex = 0;

    @Column(nullable = false)
    private int currentTurnParticipantIndex = 0;

    @Column(nullable = false)
    private boolean finished = false;

    // Null for "Pick my own". Set for "Random" - gridIds instead grows
    // lazily, one entry per round, as that round's starting player chooses
    // from pendingChoiceIds - see GridBattleRoomState's identical fields for
    // the full reasoning.
    @Column(name = "random_total_count")
    private Integer randomTotalCount;

    @ElementCollection
    @CollectionTable(name = "imposter_battle_pending_choices", joinColumns = @JoinColumn(name = "state_id"))
    @Column(name = "grid_id")
    private Set<Long> pendingChoiceIds = new HashSet<>();

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

    public List<Long> getGridIds() {
        return gridIds;
    }

    public void setGridIds(List<Long> gridIds) {
        this.gridIds = gridIds;
    }

    public int getCurrentGridIndex() {
        return currentGridIndex;
    }

    public void setCurrentGridIndex(int currentGridIndex) {
        this.currentGridIndex = currentGridIndex;
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

    public Integer getRandomTotalCount() {
        return randomTotalCount;
    }

    public void setRandomTotalCount(Integer randomTotalCount) {
        this.randomTotalCount = randomTotalCount;
    }

    public Set<Long> getPendingChoiceIds() {
        return pendingChoiceIds;
    }

    public void setPendingChoiceIds(Set<Long> pendingChoiceIds) {
        this.pendingChoiceIds = pendingChoiceIds;
    }
}
