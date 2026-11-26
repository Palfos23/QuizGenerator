package com.quizapp.repository;

import com.quizapp.model.League;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LeagueRepository extends JpaRepository<League, Long> {
    Optional<League> findByCode(String code);
    boolean existsByCode(String code);
    List<League> findByOwnerUserId(Long ownerUserId);
}
