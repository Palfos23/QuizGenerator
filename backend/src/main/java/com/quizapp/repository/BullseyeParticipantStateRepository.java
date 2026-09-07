package com.quizapp.repository;

import com.quizapp.model.BullseyeParticipantState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BullseyeParticipantStateRepository extends JpaRepository<BullseyeParticipantState, Long> {
    List<BullseyeParticipantState> findByRoomState_Id(Long roomStateId);
}
