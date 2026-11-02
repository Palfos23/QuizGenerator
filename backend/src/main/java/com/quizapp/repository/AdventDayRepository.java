package com.quizapp.repository;

import com.quizapp.model.AdventDay;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AdventDayRepository extends JpaRepository<AdventDay, Long> {

    List<AdventDay> findBySeasonYearOrderByDayNumberAsc(int seasonYear);

    Optional<AdventDay> findBySeasonYearAndDayNumber(int seasonYear, int dayNumber);
}
