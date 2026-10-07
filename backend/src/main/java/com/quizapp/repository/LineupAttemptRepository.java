package com.quizapp.repository;

import com.quizapp.model.LineupAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LineupAttemptRepository extends JpaRepository<LineupAttempt, Long> {
    Optional<LineupAttempt> findByLineup_IdAndUser_Email(Long lineupId, String email);
    List<LineupAttempt> findByLineup_IdInAndUser_Email(List<Long> lineupIds, String email);
    List<LineupAttempt> findByLineup_Id(Long lineupId);
    // For the weekly standings - see GridAttemptRepository#findByGrid_WeekStartDateBetween.
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"user", "lineup", "solvedEntryIds"})
    List<LineupAttempt> findByLineup_WeekStartDateBetween(java.time.LocalDate from, java.time.LocalDate to);
    // For account export/deletion - every attempt this user has, across all lineups.
    List<LineupAttempt> findByUser_Email(String email);
}
