package com.quizapp.repository;

import com.quizapp.model.TensionQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TensionQuestionRepository extends JpaRepository<TensionQuestion, Long> {
    List<TensionQuestion> findByMainCategoryIgnoreCase(String mainCategory);

    @Query("SELECT DISTINCT q.mainCategory FROM TensionQuestion q " +
           "WHERE q.mainCategory IS NOT NULL AND q.mainCategory <> '' ORDER BY q.mainCategory")
    List<String> findDistinctMainCategories();

    // ID-only queries for random selection - selecting just the ID column never
    // touches the answer collections, so this stays cheap regardless of how many
    // questions exist, unlike loading full entities just to pick a few at random.
    @Query("SELECT q.id FROM TensionQuestion q")
    List<Long> findAllIds();

    @Query("SELECT q.id FROM TensionQuestion q WHERE lower(q.mainCategory) = lower(:category)")
    List<Long> findIdsByMainCategoryIgnoreCase(String category);

    // For the admin list specifically - counts are computed in the query itself,
    // so listing every question never has to load (or lazily trigger a query
    // for) any question's actual answer entries just to show how many there are.
    @Query("SELECT q.id as id, q.title as title, q.mainCategory as mainCategory, " +
           "q.answersCategory as answersCategory, q.source as source, " +
           "(SELECT COUNT(a) FROM TensionAnswerEntry a WHERE a.question = q AND a.tension = false) as safeCount, " +
           "(SELECT COUNT(a) FROM TensionAnswerEntry a WHERE a.question = q AND a.tension = true) as tensionCount " +
           "FROM TensionQuestion q")
    List<TensionQuestionSummaryProjection> findAllSummaries();
}
