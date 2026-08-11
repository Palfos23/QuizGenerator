package com.quizapp.repository;

import com.quizapp.model.GridCandidate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

public interface GridCandidateRepository extends JpaRepository<GridCandidate, Long> {
    boolean existsByAthlete_Id(Long athleteId);

    // For batch-loading candidates across many grids in one query, instead of
    // relying on each grid's own lazy-loaded candidates collection (one query
    // per grid when looping over a list of grids).
    java.util.List<GridCandidate> findByGrid_IdIn(java.util.List<Long> gridIds);

    // Direct delete statement, deliberately bypassing Hibernate's collection-based
    // removal (load the collection, remove an element, let orphanRemoval figure out
    // the SQL) - that path proved unreliable for this exact scenario across several
    // attempts. A straightforward DELETE ... WHERE athlete_id = ? has no ambiguity
    // for Hibernate to get wrong.
    @Transactional
    long deleteByAthlete_Id(Long athleteId);
}
