package com.quizapp.repository;

import com.quizapp.model.FiveOhOneEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface FiveOhOneEntryRepository extends JpaRepository<FiveOhOneEntry, Long> {
    boolean existsByAthlete_Id(Long athleteId);

    // For reporting which 501 categories reference a subject before it's
    // deleted (see AthleteService#findUsage).
    List<FiveOhOneEntry> findByAthlete_Id(Long athleteId);

    // Direct delete statement, same reasoning as GridCandidateRepository's own
    // copy of this comment - bypasses Hibernate's collection-based removal,
    // which has proven unreliable for this exact scenario.
    @Transactional
    long deleteByAthlete_Id(Long athleteId);

    // Same reasoning again, this time for deleting a whole category: entries is
    // mapped cascade=ALL/orphanRemoval, so Hibernate would otherwise issue one
    // DELETE per entry when the category itself is removed - a category with a
    // few hundred subjects made deleting it noticeably slow. One statement here
    // instead, run before the (now childless) category delete.
    @Transactional
    long deleteByCategory_Id(Long categoryId);
}
