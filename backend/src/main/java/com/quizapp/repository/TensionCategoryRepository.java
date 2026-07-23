package com.quizapp.repository;

import com.quizapp.model.TensionAnswerCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TensionCategoryRepository extends JpaRepository<TensionAnswerCategory, Long> {
    Optional<TensionAnswerCategory> findByNameIgnoreCase(String name);
}
