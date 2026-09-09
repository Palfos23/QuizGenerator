package com.quizapp.repository;

import com.quizapp.model.GameRoomParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

/** See RoomService#touch for why this exists as its own repository rather
 *  than just mutating whatever GameRoomParticipant entity a caller already
 *  has in hand. */
public interface GameRoomParticipantRepository extends JpaRepository<GameRoomParticipant, Long> {
}
