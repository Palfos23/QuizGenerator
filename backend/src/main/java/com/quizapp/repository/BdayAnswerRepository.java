package com.quizapp.repository;

import com.quizapp.model.BdayAnswer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BdayAnswerRepository extends JpaRepository<BdayAnswer, Long> {
}
