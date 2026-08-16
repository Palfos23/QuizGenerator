package com.quizapp.repository;

import com.quizapp.model.AthleteDescription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AthleteDescriptionRepository extends JpaRepository<AthleteDescription, Long> {
    List<AthleteDescription> findByAthlete_Id(Long athleteId);

    // For batch-loading a whole list's worth of athletes' descriptions in one
    // query, instead of one query per athlete - same reasoning as the photo
    // repository's equivalent method.
    List<AthleteDescription> findByAthlete_IdIn(List<Long> athleteIds);

    void deleteByAthlete_Id(Long athleteId);
}
