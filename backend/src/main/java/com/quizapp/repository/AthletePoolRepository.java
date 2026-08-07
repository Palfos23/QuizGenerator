package com.quizapp.repository;

import com.quizapp.model.AthletePool;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

public interface AthletePoolRepository extends JpaRepository<AthletePool, Long> {
    @Modifying
    @Transactional
    @Query("UPDATE AthletePool p SET p.sport = :newName WHERE p.sport = :oldName")
    int renameSport(String oldName, String newName);

    boolean existsBySport(String sport);
}
