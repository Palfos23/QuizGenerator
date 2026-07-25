package com.quizapp.repository;

import com.quizapp.model.TensionRoundAnswer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TensionRoundAnswerRepository extends JpaRepository<TensionRoundAnswer, Long> {
    List<TensionRoundAnswer> findByRoomState_Id(Long roomStateId);
    Optional<TensionRoundAnswer> findByRoomState_IdAndParticipant_Id(Long roomStateId, Long participantId);
    void deleteByRoomState_Id(Long roomStateId);
}
