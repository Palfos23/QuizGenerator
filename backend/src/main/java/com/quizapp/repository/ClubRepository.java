package com.quizapp.repository;

import com.quizapp.model.Club;
import com.quizapp.model.Sport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClubRepository extends JpaRepository<Club, Long> {
    List<Club> findBySport(Sport sport);
    List<Club> findBySportAndNameContainingIgnoreCase(Sport sport, String namePart);
}
