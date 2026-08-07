package com.quizapp.repository;

import com.quizapp.model.GridCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GridCategoryRepository extends JpaRepository<GridCategory, Long> {
    boolean existsByNameIgnoreCase(String name);
    List<GridCategory> findAllByOrderByNameAsc();
}
