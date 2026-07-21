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
}
