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
}
