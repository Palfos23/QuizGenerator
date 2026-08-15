package com.quizapp.repository;

import com.quizapp.model.ImposterParticipantState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ImposterParticipantStateRepository extends JpaRepository<ImposterParticipantState, Long> {
    List<ImposterParticipantState> findByRoomState_Id(Long roomStateId);
    Optional<ImposterParticipantState> findByRoomState_IdAndParticipant_Id(Long roomStateId, Long participantId);
    void deleteByRoomState_Id(Long roomStateId);
}
