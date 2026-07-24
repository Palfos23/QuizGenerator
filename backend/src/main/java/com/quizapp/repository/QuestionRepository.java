package com.quizapp.repository;

import com.quizapp.model.Language;
import com.quizapp.model.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    List<Question> findByLanguage(Language language);

    @Query("SELECT DISTINCT q.category FROM Question q ORDER BY q.category")
    List<String> findAllDistinctCategories();

    /**
     * Filters at the database level instead of loading every question in a language
     * into memory first - matters once the bank is large, since this runs on every
     * quiz generation, discard/replace, and add-more-questions call.
     */
    @Query("SELECT q FROM Question q WHERE q.language = :language " +
           "AND q.difficultyLevel BETWEEN :minDifficulty AND :maxDifficulty " +
           "AND LOWER(q.category) = LOWER(:category)")
    List<Question> findCandidates(@org.springframework.data.repository.query.Param("language") Language language,
                                   @org.springframework.data.repository.query.Param("minDifficulty") int minDifficulty,
                                   @org.springframework.data.repository.query.Param("maxDifficulty") int maxDifficulty,
                                   @org.springframework.data.repository.query.Param("category") String category);
}
