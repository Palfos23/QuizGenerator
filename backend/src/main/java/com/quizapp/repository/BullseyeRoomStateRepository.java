package com.quizapp.repository;

import com.quizapp.model.BullseyeRoomState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BullseyeRoomStateRepository extends JpaRepository<BullseyeRoomState, Long> {
    Optional<BullseyeRoomState> findByRoom_Id(Long roomId);
}
