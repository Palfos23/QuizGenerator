package com.quizapp.repository;

import com.quizapp.model.LineupCandidate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

// Mirrors GridCandidateRepository - see that interface for why the delete is
// a direct statement rather than collection-based removal.
public interface LineupCandidateRepository extends JpaRepository<LineupCandidate, Long> {
    boolean existsByAthlete_Id(Long athleteId);

    @Transactional
    long deleteByAthlete_Id(Long athleteId);
}
