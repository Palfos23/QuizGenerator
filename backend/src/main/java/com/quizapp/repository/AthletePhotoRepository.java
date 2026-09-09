package com.quizapp.repository;

import com.quizapp.model.AthletePhoto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AthletePhotoRepository extends JpaRepository<AthletePhoto, Long> {
    // Ascending id (= upload order) rather than a plain derived query with no
    // ORDER BY at all - the admin athlete editor renders this as a fixed
    // list of photo slots, and with no guaranteed order a reload could show
    // them in a different arrangement than the admin left them in.
    List<AthletePhoto> findByAthlete_IdOrderByIdAsc(Long athleteId);

    // For batch-loading a whole list's worth of athletes' photos in one query,
    // instead of one query per athlete - critical once the subjects list is
    // in the thousands. Ordered for the same reason as the single-athlete
    // version above - AthleteService#toDtosWithPhotos groups this by athlete
    // with Collectors.mapping(...).toList(), which preserves each group's
    // photos in the order they were encountered in this query's own result.
    List<AthletePhoto> findByAthlete_IdInOrderByIdAsc(List<Long> athleteIds);

    void deleteByAthlete_Id(Long athleteId);
}
