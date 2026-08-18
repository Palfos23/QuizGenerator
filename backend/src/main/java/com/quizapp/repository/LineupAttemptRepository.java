package com.quizapp.repository;

import com.quizapp.model.LineupAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LineupAttemptRepository extends JpaRepository<LineupAttempt, Long> {
    Optional<LineupAttempt> findByLineup_IdAndUser_Email(Long lineupId, String email);
    List<LineupAttempt> findByLineup_IdInAndUser_Email(List<Long> lineupIds, String email);
    List<LineupAttempt> findByLineup_Id(Long lineupId);
}
