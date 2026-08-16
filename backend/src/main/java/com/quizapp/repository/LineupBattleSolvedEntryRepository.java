package com.quizapp.repository;

import com.quizapp.model.LineupBattleSolvedEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LineupBattleSolvedEntryRepository extends JpaRepository<LineupBattleSolvedEntry, Long> {
    List<LineupBattleSolvedEntry> findByRoomState_Id(Long roomStateId);
    void deleteByRoomState_Id(Long roomStateId);
}
