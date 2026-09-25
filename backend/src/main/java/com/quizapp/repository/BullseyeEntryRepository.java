package com.quizapp.repository;

import com.quizapp.model.BullseyeEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface BullseyeEntryRepository extends JpaRepository<BullseyeEntry, Long> {
    boolean existsByAthlete_Id(Long athleteId);

    // For reporting which Bullseye questions reference a subject before it's
    // deleted (see AthleteService#findUsage) - needs the parent question, not
    // just a yes/no.
    List<BullseyeEntry> findByAthlete_Id(Long athleteId);

    // Direct delete statement, same reasoning as GridCandidateRepository's own
    // copy of this comment - bypasses Hibernate's collection-based removal,
    // which has proven unreliable for this exact scenario.
    @Transactional
    long deleteByAthlete_Id(Long athleteId);
}
