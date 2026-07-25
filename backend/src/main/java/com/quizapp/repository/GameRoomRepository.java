package com.quizapp.repository;

import com.quizapp.model.GameRoom;
import com.quizapp.model.RoomStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface GameRoomRepository extends JpaRepository<GameRoom, Long> {
    Optional<GameRoom> findByRoomCode(String roomCode);
    List<GameRoom> findByStatusAndCreatedAtBefore(RoomStatus status, Instant cutoff);
}
