package com.quizapp.repository;

import com.quizapp.model.ImposterFlippedTile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ImposterFlippedTileRepository extends JpaRepository<ImposterFlippedTile, Long> {
    List<ImposterFlippedTile> findByRoomState_Id(Long roomStateId);
    void deleteByRoomState_Id(Long roomStateId);
}
