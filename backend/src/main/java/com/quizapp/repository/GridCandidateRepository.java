package com.quizapp.repository;

import com.quizapp.model.GridCandidate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GridCandidateRepository extends JpaRepository<GridCandidate, Long> {
    boolean existsByAthlete_Id(Long athleteId);
}
