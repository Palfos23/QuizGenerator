package com.quizapp.repository;

import com.quizapp.model.FiveOhOneThrow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FiveOhOneThrowRepository extends JpaRepository<FiveOhOneThrow, Long> {
    List<FiveOhOneThrow> findByRoomState_IdOrderByIdAsc(Long roomStateId);
    void deleteByRoomState_Id(Long roomStateId);
    // For kicking a single participant mid-game - see RoomController#kick.
    void deleteByThrownBy_Id(Long participantId);
}
