package com.quizapp.repository;

import com.quizapp.model.BdayTeam;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BdayTeamRepository extends JpaRepository<BdayTeam, Long> {
    List<BdayTeam> findAllByOrderByNameAsc();
    boolean existsByNameIgnoreCase(String name);
}
