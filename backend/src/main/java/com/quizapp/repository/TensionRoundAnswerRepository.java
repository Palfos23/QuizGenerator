package com.quizapp.repository;

import com.quizapp.model.TensionRoundAnswer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TensionRoundAnswerRepository extends JpaRepository<TensionRoundAnswer, Long> {
    // Ascending id order = submission order - the "answered so far" panel
    // (TensionOnlineService#getState -> dto.setAnswersSoFar) shows players in
    // the order they actually answered, same intent as
    // BullseyeRoundAnswerRepository's identical ordering. Left as a plain
    // derived query (no ORDER BY) this had the same bug fixed elsewhere this
    // session for player cards: no ORDER BY means no guaranteed row order at
    // all, and it can visibly change after a write to this table - so instead
    // of just being wrong sometimes, "answered so far" could show players in
    // an order that never actually happened.
    List<TensionRoundAnswer> findByRoomState_IdOrderByIdAsc(Long roomStateId);
    Optional<TensionRoundAnswer> findByRoomState_IdAndParticipant_Id(Long roomStateId, Long participantId);
    void deleteByRoomState_Id(Long roomStateId);
}
