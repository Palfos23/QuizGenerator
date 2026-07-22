package com.quizapp.repository;

import com.quizapp.model.Athlete;
import com.quizapp.model.Sport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AthleteRepository extends JpaRepository<Athlete, Long> {
    List<Athlete> findBySport(Sport sport);
    List<Athlete> findBySportAndTeamIgnoreCase(Sport sport, String team);
    List<Athlete> findBySportAndNameContainingIgnoreCase(Sport sport, String namePart);
}
