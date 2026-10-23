package com.quizapp.repository;

import com.quizapp.model.BdayAnswer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BdayAnswerRepository extends JpaRepository<BdayAnswer, Long> {

    java.util.List<BdayAnswer> findByGuest_IdOrderByIdAsc(Long guestId);

    // Resetting a guest throws away what they submitted.
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    void deleteByGuest_Id(Long guestId);
}
