package com.quizapp.service;

import com.quizapp.model.GameRoom;
import com.quizapp.model.RoomGameType;
import com.quizapp.model.RoomStatus;
import com.quizapp.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class RoomCleanupService {

    private static final Logger log = LoggerFactory.getLogger(RoomCleanupService.class);

    private final GameRoomRepository gameRoomRepository;
    private final GridBattleRoomStateRepository gridBattleRoomStateRepository;
    private final GridBattleParticipantStateRepository gridBattleParticipantStateRepository;
    private final GridBattleSolvedEntryRepository gridBattleSolvedEntryRepository;
    private final TensionRoomStateRepository tensionRoomStateRepository;
    private final TensionParticipantStateRepository tensionParticipantStateRepository;
    private final TensionRoundAnswerRepository tensionRoundAnswerRepository;
    private final LineupBattleRoomStateRepository lineupBattleRoomStateRepository;
    private final LineupBattleParticipantStateRepository lineupBattleParticipantStateRepository;
    private final LineupBattleSolvedEntryRepository lineupBattleSolvedEntryRepository;
    private final ImposterRoomStateRepository imposterRoomStateRepository;
    private final ImposterParticipantStateRepository imposterParticipantStateRepository;
    private final ImposterFlippedTileRepository imposterFlippedTileRepository;
    private final FiveOhOneRoomStateRepository fiveOhOneRoomStateRepository;
    private final FiveOhOneParticipantStateRepository fiveOhOneParticipantStateRepository;
    private final FiveOhOneThrowRepository fiveOhOneThrowRepository;
    private final BullseyeRoomStateRepository bullseyeRoomStateRepository;
    private final BullseyeParticipantStateRepository bullseyeParticipantStateRepository;
    private final BullseyeRoundAnswerRepository bullseyeRoundAnswerRepository;
    private final FlashbackRoomStateRepository flashbackRoomStateRepository;
    private final FlashbackParticipantStateRepository flashbackParticipantStateRepository;
    private final FlashbackRoundGuessRepository flashbackRoundGuessRepository;

    public RoomCleanupService(GameRoomRepository gameRoomRepository,
                               GridBattleRoomStateRepository gridBattleRoomStateRepository,
                               GridBattleParticipantStateRepository gridBattleParticipantStateRepository,
                               GridBattleSolvedEntryRepository gridBattleSolvedEntryRepository,
                               TensionRoomStateRepository tensionRoomStateRepository,
                               TensionParticipantStateRepository tensionParticipantStateRepository,
                               TensionRoundAnswerRepository tensionRoundAnswerRepository,
                               LineupBattleRoomStateRepository lineupBattleRoomStateRepository,
                               LineupBattleParticipantStateRepository lineupBattleParticipantStateRepository,
                               LineupBattleSolvedEntryRepository lineupBattleSolvedEntryRepository,
                               ImposterRoomStateRepository imposterRoomStateRepository,
                               ImposterParticipantStateRepository imposterParticipantStateRepository,
                               ImposterFlippedTileRepository imposterFlippedTileRepository,
                               FiveOhOneRoomStateRepository fiveOhOneRoomStateRepository,
                               FiveOhOneParticipantStateRepository fiveOhOneParticipantStateRepository,
                               FiveOhOneThrowRepository fiveOhOneThrowRepository,
                               BullseyeRoomStateRepository bullseyeRoomStateRepository,
                               BullseyeParticipantStateRepository bullseyeParticipantStateRepository,
                               BullseyeRoundAnswerRepository bullseyeRoundAnswerRepository,
                               FlashbackRoomStateRepository flashbackRoomStateRepository,
                               FlashbackParticipantStateRepository flashbackParticipantStateRepository,
                               FlashbackRoundGuessRepository flashbackRoundGuessRepository) {
        this.gameRoomRepository = gameRoomRepository;
        this.gridBattleRoomStateRepository = gridBattleRoomStateRepository;
        this.gridBattleParticipantStateRepository = gridBattleParticipantStateRepository;
        this.gridBattleSolvedEntryRepository = gridBattleSolvedEntryRepository;
        this.tensionRoomStateRepository = tensionRoomStateRepository;
        this.tensionParticipantStateRepository = tensionParticipantStateRepository;
        this.tensionRoundAnswerRepository = tensionRoundAnswerRepository;
        this.lineupBattleRoomStateRepository = lineupBattleRoomStateRepository;
        this.lineupBattleParticipantStateRepository = lineupBattleParticipantStateRepository;
        this.lineupBattleSolvedEntryRepository = lineupBattleSolvedEntryRepository;
        this.imposterRoomStateRepository = imposterRoomStateRepository;
        this.imposterParticipantStateRepository = imposterParticipantStateRepository;
        this.imposterFlippedTileRepository = imposterFlippedTileRepository;
        this.fiveOhOneRoomStateRepository = fiveOhOneRoomStateRepository;
        this.fiveOhOneParticipantStateRepository = fiveOhOneParticipantStateRepository;
        this.fiveOhOneThrowRepository = fiveOhOneThrowRepository;
        this.bullseyeRoomStateRepository = bullseyeRoomStateRepository;
        this.bullseyeParticipantStateRepository = bullseyeParticipantStateRepository;
        this.bullseyeRoundAnswerRepository = bullseyeRoundAnswerRepository;
        this.flashbackRoomStateRepository = flashbackRoomStateRepository;
        this.flashbackParticipantStateRepository = flashbackParticipantStateRepository;
        this.flashbackRoundGuessRepository = flashbackRoundGuessRepository;
    }

    // Runs once an hour. Thresholds: finished games kept 24h (nothing currently lets
    // anyone look back at a past game anyway), never-started rooms kept 2h (nobody
    // hit "start" - safe to assume abandoned), and stuck-in-progress games kept 24h
    // (someone likely disconnected and never came back).
    //
    // Each room is deleted in its own transaction (see deleteStale) - previously this
    // whole sweep ran as one transaction, so a single room whose game-specific child
    // rows weren't being cleaned up first (a ConstraintViolationException from
    // game_room_participants' FK) rolled back the ENTIRE hourly batch, silently
    // wedging cleanup for every other stale room too, every single hour, until that
    // one room was fixed. One room's per-game-cleanup bug should never block the rest.
    @Scheduled(fixedRate = 60 * 60 * 1000)
    public void cleanup() {
        Instant now = Instant.now();
        int removed = 0;
        removed += deleteStale(RoomStatus.FINISHED, now.minus(24, ChronoUnit.HOURS));
        removed += deleteStale(RoomStatus.WAITING, now.minus(2, ChronoUnit.HOURS));
        removed += deleteStale(RoomStatus.IN_PROGRESS, now.minus(24, ChronoUnit.HOURS));
        if (removed > 0) {
            log.info("Room cleanup: removed {} stale online room(s)", removed);
        }
    }

    private int deleteStale(RoomStatus status, Instant cutoff) {
        List<Long> staleIds = gameRoomRepository.findByStatusAndCreatedAtBefore(status, cutoff)
                .stream().map(GameRoom::getId).toList();
        int removed = 0;
        for (Long id : staleIds) {
            try {
                deleteRoomById(id);
                removed++;
            } catch (Exception e) {
                log.error("Room cleanup: failed to delete stale room {} - leaving it for the next run", id, e);
            }
        }
        return removed;
    }

    @Transactional
    public void deleteRoomById(Long roomId) {
        gameRoomRepository.findById(roomId).ifPresent(this::deleteRoomData);
    }

    private void deleteRoomData(GameRoom room) {
        if (room.getGameType() == RoomGameType.GRID_BATTLE) {
            gridBattleRoomStateRepository.findByRoom_Id(room.getId()).ifPresent(state -> {
                gridBattleSolvedEntryRepository.deleteByRoomState_Id(state.getId());
                gridBattleParticipantStateRepository.deleteByRoomState_Id(state.getId());
                gridBattleRoomStateRepository.delete(state);
            });
        } else if (room.getGameType() == RoomGameType.TENSION) {
            tensionRoomStateRepository.findByRoom_Id(room.getId()).ifPresent(state -> {
                tensionRoundAnswerRepository.deleteByRoomState_Id(state.getId());
                tensionParticipantStateRepository.deleteByRoomState_Id(state.getId());
                tensionRoomStateRepository.delete(state);
            });
        } else if (room.getGameType() == RoomGameType.STARTING_XI_BATTLE) {
            lineupBattleRoomStateRepository.findByRoom_Id(room.getId()).ifPresent(state -> {
                lineupBattleSolvedEntryRepository.deleteByRoomState_Id(state.getId());
                lineupBattleParticipantStateRepository.deleteByRoomState_Id(state.getId());
                lineupBattleRoomStateRepository.delete(state);
            });
        } else if (room.getGameType() == RoomGameType.IMPOSTER) {
            imposterRoomStateRepository.findByRoom_Id(room.getId()).ifPresent(state -> {
                imposterFlippedTileRepository.deleteByRoomState_Id(state.getId());
                imposterParticipantStateRepository.deleteByRoomState_Id(state.getId());
                imposterRoomStateRepository.delete(state);
            });
        } else if (room.getGameType() == RoomGameType.FIVE_O_ONE) {
            fiveOhOneRoomStateRepository.findByRoom_Id(room.getId()).ifPresent(state -> {
                fiveOhOneThrowRepository.deleteByRoomState_Id(state.getId());
                fiveOhOneParticipantStateRepository.deleteByRoomState_Id(state.getId());
                fiveOhOneRoomStateRepository.delete(state);
            });
        } else if (room.getGameType() == RoomGameType.BULLSEYE) {
            bullseyeRoomStateRepository.findByRoom_Id(room.getId()).ifPresent(state -> {
                bullseyeRoundAnswerRepository.deleteByRoomState_Id(state.getId());
                bullseyeParticipantStateRepository.deleteByRoomState_Id(state.getId());
                bullseyeRoomStateRepository.delete(state);
            });
        } else if (room.getGameType() == RoomGameType.FLASHBACK) {
            flashbackRoomStateRepository.findByRoom_Id(room.getId()).ifPresent(state -> {
                flashbackRoundGuessRepository.deleteByRoomState_Id(state.getId());
                flashbackParticipantStateRepository.deleteByRoomState_Id(state.getId());
                flashbackRoomStateRepository.delete(state);
            });
        }
        // GameRoomParticipant rows cascade automatically (cascade=ALL, orphanRemoval=true on GameRoom.participants) -
        // but only once every OTHER table referencing them (the per-game participant-state rows just deleted above)
        // is gone first, which is exactly what the branches above exist to guarantee.
        gameRoomRepository.delete(room);
    }
}
