package com.quizapp.repository;

import com.quizapp.model.BdayMarioKartTime;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BdayMarioKartTimeRepository extends JpaRepository<BdayMarioKartTime, Long> {
    Optional<BdayMarioKartTime> findByGuest_Id(Long guestId);
    void deleteByGuest_Id(Long guestId);
}
