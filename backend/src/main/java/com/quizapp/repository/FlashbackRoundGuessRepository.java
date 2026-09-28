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
    // Guards against double-submitting a guess on the same hint now that
    // there's no turn order serializing who goes next - see
    // FlashbackOnlineService#submitGuess.
    boolean existsByRoomState_IdAndHintIndexAndParticipant_Id(Long roomStateId, int hintIndex, Long participantId);
    void deleteByRoomState_Id(Long roomStateId);
    // For kicking a single participant mid-game - see RoomController#kick.
    void deleteByParticipant_Id(Long participantId);
}
