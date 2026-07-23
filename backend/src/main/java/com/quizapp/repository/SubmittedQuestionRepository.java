package com.quizapp.repository;

import com.quizapp.model.Language;
import com.quizapp.model.SubmittedQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubmittedQuestionRepository extends JpaRepository<SubmittedQuestion, Long> {
    List<SubmittedQuestion> findBySubmittedBy_EmailOrderByCreatedAtDesc(String email);
    List<SubmittedQuestion> findAllByOrderByCreatedAtDesc();
    List<SubmittedQuestion> findBySubmittedBy_EmailAndLanguage(String email, Language language);
}
