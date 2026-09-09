package com.quizapp.repository;

import com.quizapp.model.TensionParticipantState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TensionParticipantStateRepository extends JpaRepository<TensionParticipantState, Long> {

    /** See GridBattleParticipantStateRepository#findByRoomState_Id's comment -
     *  same fix, same reason. */
    @Query("SELECT s FROM TensionParticipantState s WHERE s.roomState.id = :roomStateId ORDER BY s.participant.joinOrder ASC")
    List<TensionParticipantState> findByRoomState_Id(@Param("roomStateId") Long roomStateId);

    Optional<TensionParticipantState> findByRoomState_IdAndParticipant_Id(Long roomStateId, Long participantId);
    void deleteByRoomState_Id(Long roomStateId);
}
