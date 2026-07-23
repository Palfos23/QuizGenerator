package com.quizapp.repository;

import com.quizapp.model.QuizTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuizTemplateRepository extends JpaRepository<QuizTemplate, Long> {
}
