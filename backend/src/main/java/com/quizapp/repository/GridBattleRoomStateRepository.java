package com.quizapp.repository;

import com.quizapp.model.GridBattleRoomState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GridBattleRoomStateRepository extends JpaRepository<GridBattleRoomState, Long> {
    Optional<GridBattleRoomState> findByRoom_Id(Long roomId);
}
