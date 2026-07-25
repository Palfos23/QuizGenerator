package com.quizapp.repository;

import com.quizapp.model.GridBattleParticipantState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GridBattleParticipantStateRepository extends JpaRepository<GridBattleParticipantState, Long> {
    List<GridBattleParticipantState> findByRoomState_Id(Long roomStateId);
    Optional<GridBattleParticipantState> findByRoomState_IdAndParticipant_Id(Long roomStateId, Long participantId);
}
