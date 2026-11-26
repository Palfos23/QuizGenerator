package com.quizapp.repository;

import com.quizapp.model.LeagueSeason;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LeagueSeasonRepository extends JpaRepository<LeagueSeason, Long> {
    List<LeagueSeason> findByLeague_IdOrderByNumberAsc(Long leagueId);
    List<LeagueSeason> findByScheduledFalse();
    void deleteByLeague_Id(Long leagueId);
}
