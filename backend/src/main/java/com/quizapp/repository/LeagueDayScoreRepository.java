package com.quizapp.repository;

import com.quizapp.model.LeagueDayScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface LeagueDayScoreRepository extends JpaRepository<LeagueDayScore, Long> {
    Optional<LeagueDayScore> findByUserIdAndQuizDate(Long userId, LocalDate quizDate);
    List<LeagueDayScore> findByUserIdInAndQuizDateBetween(Collection<Long> userIds, LocalDate from, LocalDate to);

    @Modifying
    @Transactional
    void deleteByUserIdAndQuizDate(Long userId, LocalDate quizDate);

    @Modifying
    @Transactional
    void deleteByUserId(Long userId);
}
