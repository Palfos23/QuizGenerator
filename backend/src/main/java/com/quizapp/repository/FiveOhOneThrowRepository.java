package com.quizapp.repository;

import com.quizapp.model.FiveOhOneThrow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FiveOhOneThrowRepository extends JpaRepository<FiveOhOneThrow, Long> {
    List<FiveOhOneThrow> findByRoomState_IdOrderByIdAsc(Long roomStateId);
}
