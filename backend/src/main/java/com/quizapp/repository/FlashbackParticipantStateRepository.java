package com.quizapp.repository;

import com.quizapp.model.FlashbackParticipantState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FlashbackParticipantStateRepository extends JpaRepository<FlashbackParticipantState, Long> {

    /** See GridBattleParticipantStateRepository#findByRoomState_Id's comment -
     *  same fix, same reason. */
    @Query("SELECT s FROM FlashbackParticipantState s WHERE s.roomState.id = :roomStateId ORDER BY s.participant.joinOrder ASC")
    List<FlashbackParticipantState> findByRoomState_Id(@Param("roomStateId") Long roomStateId);
}
