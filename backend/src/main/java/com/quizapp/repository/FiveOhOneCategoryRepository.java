package com.quizapp.repository;

import com.quizapp.model.FiveOhOneCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface FiveOhOneCategoryRepository extends JpaRepository<FiveOhOneCategory, Long> {

    boolean existsBySport(String sport);

    // For cascading a Subjects category rename - see GridCategoryService#update.
    @Modifying
    @Transactional
    @Query("UPDATE FiveOhOneCategory c SET c.sport = :newName WHERE c.sport = :oldName")
    int renameSport(@Param("oldName") String oldName, @Param("newName") String newName);

    // Id/title-only reads - loading a whole category pulls in every entry it has,
    // which a "swap the room's category" doesn't need.
    @Query("SELECT c.id FROM FiveOhOneCategory c")
    List<Long> findAllIds();

    @Query("SELECT c.title FROM FiveOhOneCategory c WHERE c.id = :id")
    Optional<String> findTitleById(@Param("id") Long id);
}
