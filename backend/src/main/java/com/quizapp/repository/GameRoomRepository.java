package com.quizapp.repository;

import com.quizapp.model.GameRoom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GameRoomRepository extends JpaRepository<GameRoom, Long> {
    Optional<GameRoom> findByRoomCode(String roomCode);
}
