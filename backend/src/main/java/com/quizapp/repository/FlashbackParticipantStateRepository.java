package com.quizapp.repository;

import com.quizapp.model.FlashbackParticipantState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FlashbackParticipantStateRepository extends JpaRepository<FlashbackParticipantState, Long> {
    List<FlashbackParticipantState> findByRoomState_Id(Long roomStateId);
}
