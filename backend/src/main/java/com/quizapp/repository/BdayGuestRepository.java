package com.quizapp.repository;

import com.quizapp.model.BdayGuest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BdayGuestRepository extends JpaRepository<BdayGuest, Long> {
    List<BdayGuest> findByStatusOrderByCreatedAtAsc(BdayGuest.Status status);
    List<BdayGuest> findByStatusOrderByScoreDescCompletedAtAsc(BdayGuest.Status status);
    boolean existsByNameIgnoreCase(String name);
}
