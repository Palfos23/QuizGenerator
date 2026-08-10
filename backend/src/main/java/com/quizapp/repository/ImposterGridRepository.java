package com.quizapp.repository;

import com.quizapp.model.ImposterGrid;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ImposterGridRepository extends JpaRepository<ImposterGrid, Long> {
    List<ImposterGrid> findBySportOrderByCreatedAtDesc(String sport);

    List<ImposterGrid> findAllByOrderByCreatedAtDesc();
}
