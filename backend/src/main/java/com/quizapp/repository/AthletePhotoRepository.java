package com.quizapp.repository;

import com.quizapp.model.AthletePhoto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AthletePhotoRepository extends JpaRepository<AthletePhoto, Long> {
    List<AthletePhoto> findByAthlete_Id(Long athleteId);

    void deleteByAthlete_Id(Long athleteId);
}
