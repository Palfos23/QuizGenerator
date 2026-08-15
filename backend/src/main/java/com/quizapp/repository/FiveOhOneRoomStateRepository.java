package com.quizapp.repository;

import com.quizapp.model.FiveOhOneRoomState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FiveOhOneRoomStateRepository extends JpaRepository<FiveOhOneRoomState, Long> {
    Optional<FiveOhOneRoomState> findByRoom_Id(Long roomId);
}
