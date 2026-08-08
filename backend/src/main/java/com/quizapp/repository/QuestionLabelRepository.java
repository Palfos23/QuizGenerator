package com.quizapp.repository;

import com.quizapp.model.QuestionLabel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuestionLabelRepository extends JpaRepository<QuestionLabel, Long> {
    boolean existsByNameIgnoreCase(String name);
    List<QuestionLabel> findAllByOrderByNameAsc();
}
