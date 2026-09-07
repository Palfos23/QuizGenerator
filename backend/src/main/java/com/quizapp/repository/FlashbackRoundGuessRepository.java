package com.quizapp.repository;

import com.quizapp.model.FlashbackRoundGuess;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FlashbackRoundGuessRepository extends JpaRepository<FlashbackRoundGuess, Long> {
    // Every guess made so far this round, across every hint - not just the
    // current one - so the client can render the same "guessed years grouped
    // by clue" recap the pass-and-play component already shows.
    List<FlashbackRoundGuess> findByRoomState_IdOrderByIdAsc(Long roomStateId);
    List<FlashbackRoundGuess> findByRoomState_IdAndHintIndex(Long roomStateId, int hintIndex);
    void deleteByRoomState_Id(Long roomStateId);
}
