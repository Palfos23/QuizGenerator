package com.quizapp.repository;

import com.quizapp.model.Language;
import com.quizapp.model.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    List<Question> findByLanguage(Language language);

    // Batch-fetches labels for every question in one query instead of one query
    // per question - without this, listing/searching questions triggers an N+1
    // (one extra query per question just to lazily load its labels), which gets
    // slower the bigger the question bank grows. Single collection fetch join,
    // not two at once, so no risk of the cartesian-product blowup that applies
    // when joining multiple collections of the same entity simultaneously.
    @Query("SELECT DISTINCT q FROM Question q LEFT JOIN FETCH q.labels WHERE q.language = :language")
    List<Question> findByLanguageWithLabels(@org.springframework.data.repository.query.Param("language") Language language);

    @Query("SELECT DISTINCT q FROM Question q LEFT JOIN FETCH q.labels")
    List<Question> findAllWithLabels();

    /**
     * Filters at the database level instead of loading every question in a language
     * into memory first - matters once the bank is large, since this runs on every
     * quiz generation, discard/replace, and add-more-questions call. Also batch-fetches
     * labels in the same query, for the same N+1 reason as above.
     */
    @Query("SELECT DISTINCT q FROM Question q LEFT JOIN FETCH q.labels WHERE q.language = :language " +
           "AND q.difficultyLevel BETWEEN :minDifficulty AND :maxDifficulty " +
           "AND LOWER(q.category) = LOWER(:category)")
    List<Question> findCandidates(@org.springframework.data.repository.query.Param("language") Language language,
                                   @org.springframework.data.repository.query.Param("minDifficulty") int minDifficulty,
                                   @org.springframework.data.repository.query.Param("maxDifficulty") int maxDifficulty,
                                   @org.springframework.data.repository.query.Param("category") String category);
}
