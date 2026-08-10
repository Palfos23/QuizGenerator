package com.quizapp.repository;

import com.quizapp.model.AthletePhoto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AthletePhotoRepository extends JpaRepository<AthletePhoto, Long> {
    List<AthletePhoto> findByAthlete_Id(Long athleteId);

    // For batch-loading a whole list's worth of athletes' photos in one query,
    // instead of one query per athlete - critical once the subjects list is
    // in the thousands.
    List<AthletePhoto> findByAthlete_IdIn(List<Long> athleteIds);

    void deleteByAthlete_Id(Long athleteId);
}
