package com.quizapp.repository;

import com.quizapp.model.FiveOhOneParticipantState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FiveOhOneParticipantStateRepository extends JpaRepository<FiveOhOneParticipantState, Long> {
    List<FiveOhOneParticipantState> findByRoomState_Id(Long roomStateId);
    Optional<FiveOhOneParticipantState> findByRoomState_IdAndParticipant_Id(Long roomStateId, Long participantId);
}
