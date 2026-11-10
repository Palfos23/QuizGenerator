package com.quizapp.repository;

import com.quizapp.model.DailyQuizComplaint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DailyQuizComplaintRepository extends JpaRepository<DailyQuizComplaint, Long> {

    List<DailyQuizComplaint> findByStatusOrderByCreatedAtAsc(DailyQuizComplaint.Status status);

    Optional<DailyQuizComplaint> findByAnswer_Id(Long answerId);

    List<DailyQuizComplaint> findByAnswer_Attempt_Id(Long attemptId);

    long countByAnswer_Attempt_Id(Long attemptId);

    List<DailyQuizComplaint> findByAnswer_IdInAndStatus(Collection<Long> answerIds, DailyQuizComplaint.Status status);

    // Complaints reference their answer with a non-nullable FK - they must go before the answers do.
    @Modifying
    @Transactional
    void deleteByAnswer_Attempt_Id(Long attemptId);

    @Modifying
    @Transactional
    void deleteByAnswer_Attempt_IdIn(Collection<Long> attemptIds);
}
