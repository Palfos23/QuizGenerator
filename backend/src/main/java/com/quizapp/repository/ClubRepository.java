package com.quizapp.repository;

import com.quizapp.model.Club;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface ClubRepository extends JpaRepository<Club, Long> {
    List<Club> findBySport(String sport);
    List<Club> findBySportAndNameContainingIgnoreCase(String sport, String namePart);

    @Modifying
    @Transactional
    @Query("UPDATE Club c SET c.sport = :newName WHERE c.sport = :oldName")
    int renameSport(String oldName, String newName);

    boolean existsBySport(String sport);
}
