package com.quizapp.repository;

import com.quizapp.model.BullseyeRoundAnswer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BullseyeRoundAnswerRepository extends JpaRepository<BullseyeRoundAnswer, Long> {
    // Ascending id order = submission order - needed to break elimination ties
    // by "whoever answered first", same as BullseyeGame.vue's pass-and-play
    // submissionIndex tie-break.
    List<BullseyeRoundAnswer> findByRoomState_IdOrderByIdAsc(Long roomStateId);
    void deleteByRoomState_Id(Long roomStateId);
}
