package com.quizapp.repository;

import com.quizapp.model.GridAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GridAttemptRepository extends JpaRepository<GridAttempt, Long> {
    Optional<GridAttempt> findByGrid_IdAndUser_Email(Long gridId, String email);
    List<GridAttempt> findByGrid_IdInAndUser_Email(List<Long> gridIds, String email);
}
