package com.quizapp.repository;

import com.quizapp.model.BdayWheelSpin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BdayWheelSpinRepository extends JpaRepository<BdayWheelSpin, Long> {
    Optional<BdayWheelSpin> findByGuest_Id(Long guestId);
    void deleteByGuest_Id(Long guestId);
}
