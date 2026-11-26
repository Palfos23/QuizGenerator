package com.quizapp.repository;

import com.quizapp.model.LeagueMatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface LeagueMatchRepository extends JpaRepository<LeagueMatch, Long> {
    List<LeagueMatch> findBySeason_IdOrderByQuizDateAscIdAsc(Long seasonId);
    List<LeagueMatch> findBySeason_IdIn(List<Long> seasonIds);

    @Modifying
    @Transactional
    void deleteBySeason_IdIn(List<Long> seasonIds);
}
