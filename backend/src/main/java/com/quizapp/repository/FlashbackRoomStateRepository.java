package com.quizapp.repository;

import com.quizapp.model.FlashbackRoomState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FlashbackRoomStateRepository extends JpaRepository<FlashbackRoomState, Long> {
    Optional<FlashbackRoomState> findByRoom_Id(Long roomId);
}
