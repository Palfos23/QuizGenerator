package com.quizapp.repository;

import com.quizapp.model.TensionQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TensionQuestionRepository extends JpaRepository<TensionQuestion, Long> {
    List<TensionQuestion> findByMainCategoryIgnoreCase(String mainCategory);
}
