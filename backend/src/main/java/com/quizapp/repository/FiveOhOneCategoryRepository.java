package com.quizapp.repository;

import com.quizapp.model.FiveOhOneCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FiveOhOneCategoryRepository extends JpaRepository<FiveOhOneCategory, Long> {

    // Id/title-only reads - loading a whole category pulls in every entry it has,
    // which a "swap the room's category" doesn't need.
    @Query("SELECT c.id FROM FiveOhOneCategory c")
    List<Long> findAllIds();

    @Query("SELECT c.title FROM FiveOhOneCategory c WHERE c.id = :id")
    Optional<String> findTitleById(@Param("id") Long id);
}
