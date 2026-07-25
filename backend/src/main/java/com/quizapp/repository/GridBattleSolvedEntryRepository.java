package com.quizapp.repository;

import com.quizapp.model.GridBattleSolvedEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GridBattleSolvedEntryRepository extends JpaRepository<GridBattleSolvedEntry, Long> {
    List<GridBattleSolvedEntry> findByRoomState_Id(Long roomStateId);
    void deleteByRoomState_Id(Long roomStateId);
}
