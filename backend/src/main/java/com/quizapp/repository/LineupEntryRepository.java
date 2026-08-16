package com.quizapp.repository;

import com.quizapp.model.LineupEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

// Mirrors GridEntryRepository.
public interface LineupEntryRepository extends JpaRepository<LineupEntry, Long> {
    @Transactional
    long deleteByAthlete_Id(Long athleteId);
}
