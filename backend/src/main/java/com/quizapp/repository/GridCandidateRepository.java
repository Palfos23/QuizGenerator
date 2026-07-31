package com.quizapp.repository;

import com.quizapp.model.GridCandidate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

public interface GridCandidateRepository extends JpaRepository<GridCandidate, Long> {
    boolean existsByAthlete_Id(Long athleteId);

    // Direct delete statement, deliberately bypassing Hibernate's collection-based
    // removal (load the collection, remove an element, let orphanRemoval figure out
    // the SQL) - that path proved unreliable for this exact scenario across several
    // attempts. A straightforward DELETE ... WHERE athlete_id = ? has no ambiguity
    // for Hibernate to get wrong.
    @Transactional
    long deleteByAthlete_Id(Long athleteId);
}
