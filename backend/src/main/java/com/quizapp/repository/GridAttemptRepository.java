package com.quizapp.repository;

import com.quizapp.model.GridAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GridAttemptRepository extends JpaRepository<GridAttempt, Long> {
    Optional<GridAttempt> findByGrid_IdAndUser_Email(Long gridId, String email);
    List<GridAttempt> findByGrid_IdInAndUser_Email(List<Long> gridIds, String email);
    List<GridAttempt> findByGrid_Id(Long gridId);

    // For the weekly-grid attempt cleanup job. Deliberately explicit, ordered
    // native deletes rather than loading and removing GridAttempt entities one
    // by one - the same reasoning as GridCandidateRepository's direct delete:
    // a straightforward DELETE ... WHERE has no ambiguity for Hibernate to get
    // wrong, and this can run over a large number of rows at once. Child tables
    // (an attempt's own solved/overtime entry sets) must go first to avoid a
    // foreign key violation, since they reference grid_attempts.id.
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Query(
            value = "DELETE FROM grid_attempt_solved_entries WHERE attempt_id IN (SELECT id FROM grid_attempts WHERE grid_id = :gridId)",
            nativeQuery = true)
    void deleteSolvedEntriesByGridId(Long gridId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Query(
            value = "DELETE FROM grid_attempt_overtime_entries WHERE attempt_id IN (SELECT id FROM grid_attempts WHERE grid_id = :gridId)",
            nativeQuery = true)
    void deleteOvertimeEntriesByGridId(Long gridId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Query("DELETE FROM GridAttempt a WHERE a.grid.id = :gridId")
    void deleteByGridId(Long gridId);
}
