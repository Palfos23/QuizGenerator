package com.quizapp.repository;

import com.quizapp.model.Athlete;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface AthleteRepository extends JpaRepository<Athlete, Long> {
    List<Athlete> findBySport(String sport);

    // Names only - answer-box suggestions never need the full Athlete rows, and a sport can
    // have thousands of them. The ignore-case/trim variant is only a fallback for when the
    // exact lookup finds nothing (a stored sport name that's drifted from the athletes' own).
    @Query("SELECT a.name FROM Athlete a WHERE a.sport = :sport ORDER BY a.name")
    List<String> findNamesBySport(@org.springframework.data.repository.query.Param("sport") String sport);

    @Query("SELECT a.name FROM Athlete a WHERE lower(trim(a.sport)) = lower(:sport) ORDER BY a.name")
    List<String> findNamesBySportLoose(@org.springframework.data.repository.query.Param("sport") String sport);

    @Query("SELECT DISTINCT a.sport FROM Athlete a WHERE a.sport IS NOT NULL ORDER BY a.sport")
    List<String> findDistinctSports();
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
