package com.quizapp.repository;

import com.quizapp.model.ImposterTile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface ImposterTileRepository extends JpaRepository<ImposterTile, Long> {
    boolean existsByAthlete_Id(Long athleteId);

    // For reporting which Imposter boards reference a subject before it's
    // deleted (see AthleteService#findUsage).
    List<ImposterTile> findByAthlete_Id(Long athleteId);

    // Direct delete statement, same reasoning as GridCandidateRepository's own
    // copy of this comment - bypasses Hibernate's collection-based removal,
    // which has proven unreliable for this exact scenario.
    @Transactional
    long deleteByAthlete_Id(Long athleteId);

    // replacedAthlete is only historical "who this imposter was standing in
    // for" reveal data, not a live displayed reference the way athlete is -
    // deleting that OTHER subject shouldn't block or remove this tile (whose
    // real occupant is a different, unrelated athlete), just forget which one
    // it used to be. Nullable, so this is always safe.
    @Modifying
    @Transactional
    @Query("UPDATE ImposterTile t SET t.replacedAthlete = null WHERE t.replacedAthlete.id = :athleteId")
    void clearReplacedAthlete(Long athleteId);
}
