package com.quizapp.repository;

import com.quizapp.model.PenaltyShootout;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PenaltyShootoutRepository extends JpaRepository<PenaltyShootout, Long> {
    // No summary projection here unlike GridRepository/LineupRepository - this
    // pool is admin-curated and expected to stay small (unlike the full
    // question bank), so the plain eager-fetched entity list findAll() already
    // gives isn't worth optimizing away yet. Revisit with a projection if the
    // pool ever grows large enough for that N+1-per-row cost to matter.
}
