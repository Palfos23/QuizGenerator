package com.quizapp.repository;

import com.quizapp.model.LineupBattleParticipantState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LineupBattleParticipantStateRepository extends JpaRepository<LineupBattleParticipantState, Long> {
    List<LineupBattleParticipantState> findByRoomState_Id(Long roomStateId);
    Optional<LineupBattleParticipantState> findByRoomState_IdAndParticipant_Id(Long roomStateId, Long participantId);
    void deleteByRoomState_Id(Long roomStateId);
}
