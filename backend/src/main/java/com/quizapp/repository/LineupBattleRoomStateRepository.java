package com.quizapp.repository;

import com.quizapp.model.LineupBattleRoomState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LineupBattleRoomStateRepository extends JpaRepository<LineupBattleRoomState, Long> {
    Optional<LineupBattleRoomState> findByRoom_Id(Long roomId);
}
