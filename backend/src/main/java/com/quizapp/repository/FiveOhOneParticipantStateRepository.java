package com.quizapp.repository;

import com.quizapp.model.FiveOhOneParticipantState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FiveOhOneParticipantStateRepository extends JpaRepository<FiveOhOneParticipantState, Long> {

    /** See GridBattleParticipantStateRepository#findByRoomState_Id's comment -
     *  same fix, same reason. */
    @Query("SELECT s FROM FiveOhOneParticipantState s WHERE s.roomState.id = :roomStateId ORDER BY s.participant.joinOrder ASC")
    List<FiveOhOneParticipantState> findByRoomState_Id(@Param("roomStateId") Long roomStateId);

    Optional<FiveOhOneParticipantState> findByRoomState_IdAndParticipant_Id(Long roomStateId, Long participantId);
}
