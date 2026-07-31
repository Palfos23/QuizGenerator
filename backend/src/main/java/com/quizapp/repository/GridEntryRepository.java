package com.quizapp.repository;

import com.quizapp.model.GridEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface GridEntryRepository extends JpaRepository<GridEntry, Long> {
    List<GridEntry> findByGrid_IdOrderByHintValueDesc(Long gridId);
    boolean existsByClub_Id(Long clubId);

    // Same direct-delete approach as GridCandidateRepository.deleteByAthlete_Id -
    // bypasses collection-based removal entirely.
    @Transactional
    long deleteByAthlete_Id(Long athleteId);
}
