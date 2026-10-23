package com.quizapp.repository;

import com.quizapp.model.BdayQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BdayQuestionRepository extends JpaRepository<BdayQuestion, Long> {

    List<BdayQuestion> findAllByOrderBySortOrderAscIdAsc();
}
