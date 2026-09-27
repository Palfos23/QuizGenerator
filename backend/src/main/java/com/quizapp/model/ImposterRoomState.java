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

    // Whose turn it is to flip a tile, by participant id - not a raw index
    // into room.getParticipants(). An index would silently point at a
    // different person the moment anyone earlier in that list is kicked
    // (the array shifts under it); an id stays correct regardless, and
    // getState() falls back gracefully if this exact id is no longer in the
    // room (they were the one kicked). Null only before the first board's
    // starter is picked.
    @Column(name = "current_turn_participant_id")
    private Long currentTurnParticipantId;

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

    public Long getCurrentTurnParticipantId() {
        return currentTurnParticipantId;
    }

    public void setCurrentTurnParticipantId(Long currentTurnParticipantId) {
        this.currentTurnParticipantId = currentTurnParticipantId;
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
