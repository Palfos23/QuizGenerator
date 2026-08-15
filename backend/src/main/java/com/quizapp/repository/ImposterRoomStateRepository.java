package com.quizapp.repository;

import com.quizapp.model.ImposterRoomState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ImposterRoomStateRepository extends JpaRepository<ImposterRoomState, Long> {
    Optional<ImposterRoomState> findByRoom_Id(Long roomId);
}
