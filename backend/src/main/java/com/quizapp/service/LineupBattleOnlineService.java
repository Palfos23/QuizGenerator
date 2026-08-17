package com.quizapp.service;

import com.quizapp.dto.LineupBattlePlayerStateDto;
import com.quizapp.dto.LineupBattleSlotDto;
import com.quizapp.dto.LineupBattleStateDto;
import com.quizapp.exception.ResourceNotFoundException;
import com.quizapp.model.*;
import com.quizapp.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

// Mirrors GridBattleOnlineService entry-for-entry - a Starting XI Battle
// session is structurally identical to a Grid Battle one (a room steps
// through 2-4 boards, turn rotates strictly between participants, each has
// their own lives, shared visibility once a slot is solved). The only real
// difference is what's being guessed: a Lineup's 11 formation slots instead
// of a Grid's tiles.
@Service
public class LineupBattleOnlineService {

    private static final String DEFAULT_KIT_COLOR = "#d92332";
    private static final String DEFAULT_GK_KIT_COLOR = "#f2c230";

    private final GameRoomRepository gameRoomRepository;
    private final LineupBattleRoomStateRepository roomStateRepository;
    private final LineupBattleParticipantStateRepository participantStateRepository;
    private final LineupBattleSolvedEntryRepository solvedEntryRepository;
    private final LineupRepository lineupRepository;
    private final RoomService roomService;

    public LineupBattleOnlineService(GameRoomRepository gameRoomRepository,
                                      LineupBattleRoomStateRepository roomStateRepository,
                                      LineupBattleParticipantStateRepository participantStateRepository,
                                      LineupBattleSolvedEntryRepository solvedEntryRepository,
                                      LineupRepository lineupRepository, RoomService roomService) {
        this.gameRoomRepository = gameRoomRepository;
        this.roomStateRepository = roomStateRepository;
        this.participantStateRepository = participantStateRepository;
        this.solvedEntryRepository = solvedEntryRepository;
        this.lineupRepository = lineupRepository;
        this.roomService = roomService;
    }

    /** Lineup sequence is decided at room-creation time, before anyone else has joined. */
    @Transactional
    public void initializeLineupSequence(GameRoom room, List<Long> lineupIds, Integer randomCount) {
        List<Long> sequence;
        if (lineupIds != null && !lineupIds.isEmpty()) {
            sequence = lineupIds;
        } else {
            int count = (randomCount != null && randomCount >= 2 && randomCount <= 4) ? randomCount : 2;
            List<Lineup> all = lineupRepository.findAll().stream()
                    .filter(l -> !l.isExcludedFromBattle())
                    .collect(Collectors.toList());
            Collections.shuffle(all);
            sequence = all.stream().limit(count).map(Lineup::getId).collect(Collectors.toList());
        }
        if (sequence.size() < 2 || sequence.size() > 4) {
            throw new IllegalArgumentException("Choose 2-4 Starting XI boards to play.");
        }
        LineupBattleRoomState state = new LineupBattleRoomState();
        state.setRoom(room);
        state.setLineupIds(sequence);
        roomStateRepository.save(state);
    }

    @Transactional
    public void startGame(GameRoom room, String requestingEmail) {
        if (!room.getHostEmail().equals(requestingEmail)) {
            throw new IllegalStateException("Only the host can start the game.");
        }
        if (room.getParticipants().size() < 2) {
            throw new IllegalStateException("Need at least 2 players to start.");
        }
        LineupBattleRoomState state = roomStateRepository.findByRoom_Id(room.getId())
                .orElseThrow(() -> new IllegalStateException("This room isn't set up yet."));

        for (GameRoomParticipant p : room.getParticipants()) {
            LineupBattleParticipantState ps = new LineupBattleParticipantState();
            ps.setRoomState(state);
            ps.setParticipant(p);
            participantStateRepository.save(ps);
        }
        room.setStatus(RoomStatus.IN_PROGRESS);
        gameRoomRepository.save(room);
    }

    @Transactional
    public LineupBattleStateDto getState(GameRoom room, String requestingEmail) {
        GameRoomParticipant me = roomService.requireParticipant(room, requestingEmail);
        roomService.touch(me);

        LineupBattleStateDto dto = new LineupBattleStateDto();
        dto.setRoomCode(room.getRoomCode());
        dto.setStatus(room.getStatus());
        dto.setYourParticipantId(me.getId());

        if (room.getStatus() == RoomStatus.WAITING) {
            return dto;
        }

        LineupBattleRoomState state = roomStateRepository.findByRoom_Id(room.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No game state for this room"));
        List<LineupBattleParticipantState> participantStates = participantStateRepository.findByRoomState_Id(state.getId());

        dto.setTotalLineups(state.getLineupIds().size());
        dto.setCurrentLineupIndex(state.getCurrentLineupIndex());
        dto.setFinished(state.isFinished());

        if (state.isFinished()) {
            dto.setPlayers(toPlayerDtos(participantStates, Collections.emptySet()));
            return dto;
        }

        Long currentLineupId = state.getLineupIds().get(state.getCurrentLineupIndex());
        Lineup lineup = lineupRepository.findById(currentLineupId)
                .orElseThrow(() -> new ResourceNotFoundException("Starting XI board no longer exists"));
        dto.setCurrentLineupId(currentLineupId);
        dto.setLineupTitle(lineup.getTitle());
        dto.setLineupCompetition(lineup.getCompetition());
        dto.setTeamName(lineup.getTeamName());
        dto.setOpponentName(lineup.getOpponentName());
        dto.setScoreFor(lineup.getScoreFor());
        dto.setScoreAgainst(lineup.getScoreAgainst());
        dto.setFormation(lineup.getFormation());
        dto.setMaxStrikes(lineup.getMaxStrikes());
        dto.setKitColor(lineup.getKitColor() != null ? lineup.getKitColor() : DEFAULT_KIT_COLOR);
        dto.setGoalkeeperKitColor(lineup.getGoalkeeperKitColor() != null ? lineup.getGoalkeeperKitColor() : DEFAULT_GK_KIT_COLOR);

        List<LineupBattleSolvedEntry> solved = solvedEntryRepository.findByRoomState_Id(state.getId());
        Map<Long, LineupBattleSolvedEntry> solvedByEntryId = solved.stream()
                .collect(Collectors.toMap(LineupBattleSolvedEntry::getLineupEntryId, s -> s));

        Set<Long> eliminatedIds = participantStates.stream()
                .filter(ps -> ps.getLivesUsedThisLineup() >= lineup.getMaxStrikes())
                .map(ps -> ps.getParticipant().getId())
                .collect(Collectors.toSet());
        dto.setPlayers(toPlayerDtos(participantStates, eliminatedIds));

        boolean allSolved = solved.size() >= lineup.getEntries().size();
        boolean allEliminated = eliminatedIds.size() >= participantStates.size();
        dto.setLineupComplete(allSolved || allEliminated);

        // Once everyone's out of lives, reveal every remaining shirt - matching
        // solo Starting XI's "give up" behavior.
        boolean revealAll = allEliminated;

        dto.setSlots(lineup.getEntries().stream()
                .sorted(Comparator.comparingInt(LineupEntry::getSlotIndex))
                .map(e -> {
                    LineupBattleSolvedEntry s = solvedByEntryId.get(e.getId());
                    boolean isSolved = s != null;
                    boolean visible = isSolved || revealAll;
                    return new LineupBattleSlotDto(e.getId(), e.getSlotIndex(), e.getShirtNumber(), e.isCaptain(),
                            isSolved, visible ? e.getAthlete().getName() : null,
                            visible ? e.getAthlete().getPhotoUrl() : null,
                            isSolved ? s.getSolvedBy().getDisplayName() : null);
                })
                .collect(Collectors.toList()));

        if (!dto.isLineupComplete()) {
            List<GameRoomParticipant> ordered = room.getParticipants();
            int idx = state.getCurrentTurnParticipantIndex();
            for (int tries = 0; tries < ordered.size(); tries++) {
                GameRoomParticipant candidate = ordered.get(idx % ordered.size());
                if (!eliminatedIds.contains(candidate.getId())) {
                    dto.setCurrentTurnParticipantId(candidate.getId());
                    break;
                }
                idx++;
            }
        }
        return dto;
    }

    @Transactional
    public LineupBattleStateDto guess(GameRoom room, String requestingEmail, Long athleteId) {
        GameRoomParticipant me = roomService.requireParticipant(room, requestingEmail);
        LineupBattleStateDto currentView = getState(room, requestingEmail);

        if (currentView.isFinished()) throw new IllegalStateException("This game has already finished.");
        if (currentView.isLineupComplete()) throw new IllegalStateException("This board is already finished.");
        if (!me.getId().equals(currentView.getCurrentTurnParticipantId())) {
            throw new IllegalStateException("It's not your turn.");
        }

        LineupBattleRoomState state = roomStateRepository.findByRoom_Id(room.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No game state for this room"));
        Long currentLineupId = state.getLineupIds().get(state.getCurrentLineupIndex());
        Lineup lineup = lineupRepository.findById(currentLineupId)
                .orElseThrow(() -> new ResourceNotFoundException("Starting XI board no longer exists"));

        Set<Long> solvedEntryIds = solvedEntryRepository.findByRoomState_Id(state.getId()).stream()
                .map(LineupBattleSolvedEntry::getLineupEntryId).collect(Collectors.toSet());

        LineupEntry matched = lineup.getEntries().stream()
                .filter(e -> !solvedEntryIds.contains(e.getId()) && e.getAthlete().getId().equals(athleteId))
                .findFirst().orElse(null);

        LineupBattleParticipantState myState = participantStateRepository
                .findByRoomState_IdAndParticipant_Id(state.getId(), me.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No progress state for you in this room"));

        if (matched != null) {
            LineupBattleSolvedEntry se = new LineupBattleSolvedEntry();
            se.setRoomState(state);
            se.setLineupEntryId(matched.getId());
            se.setSolvedBy(me);
            solvedEntryRepository.save(se);
            myState.setTotalScore(myState.getTotalScore() + 1);
        } else {
            myState.setLivesUsedThisLineup(myState.getLivesUsedThisLineup() + 1);
        }
        participantStateRepository.save(myState);

        List<GameRoomParticipant> ordered = room.getParticipants();
        int myIndex = ordered.indexOf(me);
        state.setCurrentTurnParticipantIndex((myIndex + 1) % ordered.size());
        roomStateRepository.save(state);

        return getState(room, requestingEmail);
    }

    @Transactional
    public LineupBattleStateDto skip(GameRoom room, String requestingEmail) {
        GameRoomParticipant me = roomService.requireParticipant(room, requestingEmail);
        LineupBattleStateDto currentView = getState(room, requestingEmail);

        if (currentView.isFinished()) throw new IllegalStateException("This game has already finished.");
        if (currentView.isLineupComplete()) throw new IllegalStateException("This board is already finished.");
        if (!me.getId().equals(currentView.getCurrentTurnParticipantId())) {
            throw new IllegalStateException("It's not your turn.");
        }

        LineupBattleRoomState state = roomStateRepository.findByRoom_Id(room.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No game state for this room"));

        LineupBattleParticipantState myState = participantStateRepository
                .findByRoomState_IdAndParticipant_Id(state.getId(), me.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No progress state for you in this room"));

        // Same cost as a wrong guess - passing isn't a free way to stall.
        myState.setLivesUsedThisLineup(myState.getLivesUsedThisLineup() + 1);
        participantStateRepository.save(myState);

        List<GameRoomParticipant> ordered = room.getParticipants();
        int myIndex = ordered.indexOf(me);
        state.setCurrentTurnParticipantIndex((myIndex + 1) % ordered.size());
        roomStateRepository.save(state);

        return getState(room, requestingEmail);
    }

    @Transactional
    public LineupBattleStateDto advanceToNextLineup(GameRoom room, String requestingEmail) {
        if (!room.getHostEmail().equals(requestingEmail)) {
            throw new IllegalStateException("Only the host can move on to the next board.");
        }
        LineupBattleStateDto current = getState(room, requestingEmail);
        if (!current.isLineupComplete()) {
            throw new IllegalStateException("This board isn't finished yet.");
        }

        LineupBattleRoomState state = roomStateRepository.findByRoom_Id(room.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No game state for this room"));

        solvedEntryRepository.deleteByRoomState_Id(state.getId());
        for (LineupBattleParticipantState ps : participantStateRepository.findByRoomState_Id(state.getId())) {
            ps.setLivesUsedThisLineup(0);
            participantStateRepository.save(ps);
        }

        if (state.getCurrentLineupIndex() + 1 >= state.getLineupIds().size()) {
            state.setFinished(true);
            room.setStatus(RoomStatus.FINISHED);
            gameRoomRepository.save(room);
        } else {
            state.setCurrentLineupIndex(state.getCurrentLineupIndex() + 1);
            state.setCurrentTurnParticipantIndex(state.getCurrentLineupIndex() % room.getParticipants().size());
        }
        roomStateRepository.save(state);
        return getState(room, requestingEmail);
    }

    private List<LineupBattlePlayerStateDto> toPlayerDtos(List<LineupBattleParticipantState> states, Set<Long> eliminatedIds) {
        return states.stream().map(ps -> new LineupBattlePlayerStateDto(
                ps.getParticipant().getId(), ps.getParticipant().getDisplayName(), ps.getParticipant().getColor(),
                roomService.isConnected(ps.getParticipant()), ps.getLivesUsedThisLineup(),
                eliminatedIds.contains(ps.getParticipant().getId()), ps.getTotalScore()
        )).collect(Collectors.toList());
    }
}
