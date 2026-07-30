package com.quizapp.repository;

import com.quizapp.model.TensionAnswerCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface TensionCategoryRepository extends JpaRepository<TensionAnswerCategory, Long> {
    Optional<TensionAnswerCategory> findByNameIgnoreCase(String name);

    // For the admin list - the option count is computed in the query itself, so
    // listing every category never has to lazily load (and trigger a separate
    // query for) any category's actual option list just to show how many there are.
    @Query("SELECT c.id as id, c.name as name, SIZE(c.options) as optionCount FROM TensionAnswerCategory c")
    java.util.List<TensionCategorySummaryProjection> findAllSummaries();
}
