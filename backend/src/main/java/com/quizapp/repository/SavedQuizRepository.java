package com.quizapp.repository;

import com.quizapp.model.SavedQuiz;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SavedQuizRepository extends JpaRepository<SavedQuiz, Long> {
    List<SavedQuiz> findByOwner_EmailOrderByCreatedAtDesc(String email);
    Optional<SavedQuiz> findByIdAndOwner_Email(Long id, String email);
}
