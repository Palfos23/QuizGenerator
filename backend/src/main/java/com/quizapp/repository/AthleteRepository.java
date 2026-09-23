package com.quizapp.repository;

import com.quizapp.model.Athlete;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface AthleteRepository extends JpaRepository<Athlete, Long> {
    List<Athlete> findBySport(String sport);
    List<Athlete> findBySportAndTeamIgnoreCase(String sport, String team);
    List<Athlete> findBySportAndNameContainingIgnoreCase(String sport, String namePart);

    // For cascading a category rename - a direct bulk update, not
    // collection-based, matching the pattern already proven reliable
    // elsewhere in this project for this exact kind of operation.
    @Modifying
    @Transactional
    @Query("UPDATE Athlete a SET a.sport = :newName WHERE a.sport = :oldName")
    int renameSport(String oldName, String newName);

    boolean existsBySport(String sport);

    // Duplicate-name guard for editing a subject - IdNot excludes the subject
    // being edited itself, so renaming it back to its own current name (no-op)
    // or to a name unused elsewhere in the same category is still allowed.
    boolean existsBySportAndNameIgnoreCaseAndIdNot(String sport, String name, Long id);
}
