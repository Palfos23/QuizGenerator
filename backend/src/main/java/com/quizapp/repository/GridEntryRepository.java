package com.quizapp.repository;

import com.quizapp.model.GridEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GridEntryRepository extends JpaRepository<GridEntry, Long> {
    List<GridEntry> findByGrid_IdOrderByHintValueDesc(Long gridId);
}
