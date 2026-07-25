package com.quizapp.repository;

import com.quizapp.model.TensionRoomState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TensionRoomStateRepository extends JpaRepository<TensionRoomState, Long> {
    Optional<TensionRoomState> findByRoom_Id(Long roomId);
}
