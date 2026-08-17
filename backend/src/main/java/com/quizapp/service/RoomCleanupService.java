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
    private final FiveOhOneRoomStateRepository fiveOhOneRoomStateRepository;

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
                               FiveOhOneRoomStateRepository fiveOhOneRoomStateRepository) {
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
        this.fiveOhOneRoomStateRepository = fiveOhOneRoomStateRepository;
    }

    // Runs once an hour. Thresholds: finished games kept 24h (nothing currently lets
    // anyone look back at a past game anyway), never-started rooms kept 2h (nobody
    // hit "start" - safe to assume abandoned), and stuck-in-progress games kept 24h
    // (someone likely disconnected and never came back).
    @Scheduled(fixedRate = 60 * 60 * 1000)
    @Transactional
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
        List<GameRoom> stale = gameRoomRepository.findByStatusAndCreatedAtBefore(status, cutoff);
        for (GameRoom room : stale) {
            deleteRoomData(room);
        }
        return stale.size();
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
            // No separate participant/solved-entry child tables here - just the
            // room state itself (its grid-sequence list is a Hibernate
            // @ElementCollection, deleted automatically with the parent row).
            imposterRoomStateRepository.findByRoom_Id(room.getId())
                    .ifPresent(imposterRoomStateRepository::delete);
        } else if (room.getGameType() == RoomGameType.FIVE_O_ONE) {
            fiveOhOneRoomStateRepository.findByRoom_Id(room.getId())
                    .ifPresent(fiveOhOneRoomStateRepository::delete);
        }
        // GameRoomParticipant rows cascade automatically (cascade=ALL, orphanRemoval=true on GameRoom.participants).
        gameRoomRepository.delete(room);
    }
}
