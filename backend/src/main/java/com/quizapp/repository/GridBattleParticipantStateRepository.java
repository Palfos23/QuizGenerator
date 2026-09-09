package com.quizapp.repository;

import com.quizapp.model.GridBattleParticipantState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GridBattleParticipantStateRepository extends JpaRepository<GridBattleParticipantState, Long> {

    /**
     * Explicitly ordered by seat (GameRoomParticipant#joinOrder, set once at
     * join time and never changed again) rather than left as a plain derived
     * query - a SELECT with no ORDER BY has no guaranteed row order at all,
     * and Postgres in particular can and does return a different order after
     * a row in the result set gets UPDATEd (a non-HOT update relocates that
     * row's tuple, changing where a plain sequential/index scan encounters it
     * next time). Every one of this table's rows gets updated on essentially
     * every turn (livesUsedThisGrid/totalScore), which is exactly why player
     * cards on the frontend (which just render this list in the order it
     * comes back) were visibly swapping positions turn to turn.
     */
    @Query("SELECT s FROM GridBattleParticipantState s WHERE s.roomState.id = :roomStateId ORDER BY s.participant.joinOrder ASC")
    List<GridBattleParticipantState> findByRoomState_Id(@Param("roomStateId") Long roomStateId);

    Optional<GridBattleParticipantState> findByRoomState_IdAndParticipant_Id(Long roomStateId, Long participantId);
    void deleteByRoomState_Id(Long roomStateId);
}
