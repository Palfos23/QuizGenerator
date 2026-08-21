package com.quizapp.repository;

import com.quizapp.model.ImposterGrid;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;

public interface ImposterGridRepository extends JpaRepository<ImposterGrid, Long> {
    List<ImposterGrid> findBySportOrderByCreatedAtDesc(String sport);

    List<ImposterGrid> findAllByOrderByCreatedAtDesc();

    // Lightweight projections for the board list/picker and the battle
    // round-choice picker - ImposterGrid.tiles is FetchType.EAGER, and each
    // ImposterTile itself carries up to six more EAGER @ManyToOne associations
    // (athlete, replacedAthlete, club, selectedPhoto, and both reveal photos),
    // so hydrating full entities via the entity-returning queries above (or
    // findAll()/findAllById()) is the worst N+1 offender in the app - one
    // query per grid, times up to six more per tile. These select only the
    // counts each summary screen actually needs.
    @Query("SELECT g.id as id, g.title as title, g.description as description, g.sport as sport, " +
           "(SELECT COUNT(t) FROM ImposterTile t WHERE t.imposterGrid = g) as tileCount, " +
           "(SELECT COUNT(t) FROM ImposterTile t WHERE t.imposterGrid = g AND t.imposter = true) as imposterCount " +
           "FROM ImposterGrid g ORDER BY g.createdAt DESC")
    List<ImposterGridSummaryProjection> findSummariesOrderByCreatedAtDesc();

    @Query("SELECT g.id as id, g.title as title, g.description as description, g.sport as sport, " +
           "(SELECT COUNT(t) FROM ImposterTile t WHERE t.imposterGrid = g) as tileCount, " +
           "(SELECT COUNT(t) FROM ImposterTile t WHERE t.imposterGrid = g AND t.imposter = true) as imposterCount " +
           "FROM ImposterGrid g WHERE g.sport = :sport ORDER BY g.createdAt DESC")
    List<ImposterGridSummaryProjection> findSummariesBySportOrderByCreatedAtDesc(String sport);

    @Query("SELECT g.id FROM ImposterGrid g")
    List<Long> findAllIds();

    @Query("SELECT g.id as id, g.title as title, g.description as description, g.sport as sport, " +
           "(SELECT COUNT(t) FROM ImposterTile t WHERE t.imposterGrid = g) as tileCount, " +
           "(SELECT COUNT(t) FROM ImposterTile t WHERE t.imposterGrid = g AND t.imposter = true) as imposterCount " +
           "FROM ImposterGrid g WHERE g.id IN :ids")
    List<ImposterGridSummaryProjection> findSummariesByIdIn(Collection<Long> ids);
}
