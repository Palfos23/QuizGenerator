package com.quizapp.repository;

import com.quizapp.model.Athlete;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface AthleteRepository extends JpaRepository<Athlete, Long> {
    List<Athlete> findBySport(String sport);
    List<Athlete> findBySportAndTeamIgnoreCase(String sport, String team);
    List<Athlete> findBySportAndNameContainingIgnoreCase(String sport, String namePart);

    // For cascading a category rename - a direct bulk update, not
    // collection-based, matching the pattern already proven reliable
    // elsewhere in this project for this exact kind of operation.
    @Modifying
    @Transactional
    @Query("UPDATE Athlete a SET a.sport = :newName WHERE a.sport = :oldName")
    int renameSport(String oldName, String newName);

    boolean existsBySport(String sport);
}
