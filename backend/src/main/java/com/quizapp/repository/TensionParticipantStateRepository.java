package com.quizapp.repository;

import com.quizapp.model.TensionParticipantState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TensionParticipantStateRepository extends JpaRepository<TensionParticipantState, Long> {
    List<TensionParticipantState> findByRoomState_Id(Long roomStateId);
    Optional<TensionParticipantState> findByRoomState_IdAndParticipant_Id(Long roomStateId, Long participantId);
    void deleteByRoomState_Id(Long roomStateId);
}
