package com.quizapp.service;

import com.quizapp.dto.GridBattleEntryDto;
import com.quizapp.dto.GridBattlePlayerStateDto;
import com.quizapp.dto.GridBattleStateDto;
import com.quizapp.exception.ResourceNotFoundException;
import com.quizapp.model.*;
import com.quizapp.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class GridBattleOnlineService {

    private final GameRoomRepository gameRoomRepository;
    private final GridBattleRoomStateRepository roomStateRepository;
    private final GridBattleParticipantStateRepository participantStateRepository;
    private final GridBattleSolvedEntryRepository solvedEntryRepository;
    private final GridRepository gridRepository;
    private final RoomService roomService;

    public GridBattleOnlineService(GameRoomRepository gameRoomRepository,
                                    GridBattleRoomStateRepository roomStateRepository,
                                    GridBattleParticipantStateRepository participantStateRepository,
                                    GridBattleSolvedEntryRepository solvedEntryRepository,
                                    GridRepository gridRepository,
                                    RoomService roomService) {
        this.gameRoomRepository = gameRoomRepository;
        this.roomStateRepository = roomStateRepository;
        this.participantStateRepository = participantStateRepository;
        this.solvedEntryRepository = solvedEntryRepository;
        this.gridRepository = gridRepository;
        this.roomService = roomService;
    }

    /** Grid sequence is decided at room-creation time, before anyone else has joined. */
    @Transactional
    public void initializeGridSequence(GameRoom room, List<Long> gridIds, Integer randomCount) {
        List<Long> sequence;
        if (gridIds != null && !gridIds.isEmpty()) {
            sequence = gridIds;
        } else {
            int count = (randomCount != null && randomCount >= 2 && randomCount <= 4) ? randomCount : 2;
            List<Grid> all = gridRepository.findAll().stream()
                    .filter(g -> !g.isExcludedFromGridBattle())
                    .collect(Collectors.toList());
            Collections.shuffle(all);
            sequence = all.stream().limit(count).map(Grid::getId).collect(Collectors.toList());
        }
        if (sequence.size() < 2 || sequence.size() > 4) {
            throw new IllegalArgumentException("Choose 2-4 grids to play.");
        }
        GridBattleRoomState state = new GridBattleRoomState();
        state.setRoom(room);
        state.setGridIds(sequence);
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
        GridBattleRoomState state = roomStateRepository.findByRoom_Id(room.getId())
                .orElseThrow(() -> new IllegalStateException("This room isn't set up yet."));

        for (GameRoomParticipant p : room.getParticipants()) {
            GridBattleParticipantState ps = new GridBattleParticipantState();
            ps.setRoomState(state);
            ps.setParticipant(p);
            participantStateRepository.save(ps);
        }
        room.setStatus(RoomStatus.IN_PROGRESS);
        gameRoomRepository.save(room);
    }

    @Transactional
    public GridBattleStateDto getState(GameRoom room, String requestingEmail) {
        GameRoomParticipant me = roomService.requireParticipant(room, requestingEmail);
        roomService.touch(me);

        GridBattleStateDto dto = new GridBattleStateDto();
        dto.setRoomCode(room.getRoomCode());
        dto.setStatus(room.getStatus());
        dto.setYourParticipantId(me.getId());

        if (room.getStatus() == RoomStatus.WAITING) {
            return dto;
        }

        GridBattleRoomState state = roomStateRepository.findByRoom_Id(room.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No game state for this room"));
        List<GridBattleParticipantState> participantStates = participantStateRepository.findByRoomState_Id(state.getId());

        dto.setTotalGrids(state.getGridIds().size());
        dto.setCurrentGridIndex(state.getCurrentGridIndex());
        dto.setFinished(state.isFinished());

        if (state.isFinished()) {
            dto.setPlayers(toPlayerDtos(participantStates, Collections.emptySet()));
            return dto;
        }

        Long currentGridId = state.getGridIds().get(state.getCurrentGridIndex());
        Grid grid = gridRepository.findById(currentGridId)
                .orElseThrow(() -> new ResourceNotFoundException("Grid no longer exists"));
        dto.setCurrentGridId(currentGridId);
        dto.setGridTitle(grid.getTitle());
        dto.setGridTheme(grid.getTheme());
        dto.setMaxStrikes(grid.getMaxStrikes());

        List<GridBattleSolvedEntry> solved = solvedEntryRepository.findByRoomState_Id(state.getId());
        Map<Long, GridBattleSolvedEntry> solvedByEntryId = solved.stream()
                .collect(Collectors.toMap(GridBattleSolvedEntry::getGridEntryId, s -> s));

        Set<Long> eliminatedIds = participantStates.stream()
                .filter(ps -> ps.getLivesUsedThisGrid() >= grid.getMaxStrikes())
                .map(ps -> ps.getParticipant().getId())
                .collect(Collectors.toSet());
        dto.setPlayers(toPlayerDtos(participantStates, eliminatedIds));

        boolean allSolved = solved.size() >= grid.getEntries().size();
        boolean allEliminated = eliminatedIds.size() >= participantStates.size();
        dto.setGridComplete(allSolved || allEliminated);

        // Once everyone's out of lives, reveal every remaining tile - matching the
        // single-player Weekly Grid's "reveal" behavior - rather than leaving unsolved
        // answers hidden forever with no way to see what they were.
        boolean revealAll = allEliminated;

        dto.setEntries(grid.getEntries().stream()
                .sorted(entrySortOrderForOnline(grid))
                // Tie-broken by id - entries is a Set with no guaranteed iteration
                // order, so without this, tiles with the same hint value (e.g. two
                // players tied on goals) had no defined relative order and could
                // visibly swap position between polls despite nothing having
                // actually changed.
                .map(e -> {
                    GridBattleSolvedEntry s = solvedByEntryId.get(e.getId());
                    boolean isSolved = s != null;
                    boolean visible = isSolved || revealAll;
                    boolean photoVisible = visible || !grid.isRanked();
                    return new GridBattleEntryDto(e.getId(), e.getHintLabel(), e.getHintValue(), isSolved,
                            visible ? e.getAthlete().getName() : null,
                            photoVisible ? e.getAthlete().getPhotoUrl() : null,
                            logoUrl(e), hintColor(e),
                            isSolved ? s.getSolvedBy().getDisplayName() : null);
                })
                .collect(Collectors.toList()));

        if (!dto.isGridComplete()) {
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
    public GridBattleStateDto guess(GameRoom room, String requestingEmail, Long athleteId) {
        GameRoomParticipant me = roomService.requireParticipant(room, requestingEmail);
        GridBattleStateDto currentView = getState(room, requestingEmail);

        if (currentView.isFinished()) throw new IllegalStateException("This game has already finished.");
        if (currentView.isGridComplete()) throw new IllegalStateException("This grid is already finished.");
        if (!me.getId().equals(currentView.getCurrentTurnParticipantId())) {
            throw new IllegalStateException("It's not your turn.");
        }

        GridBattleRoomState state = roomStateRepository.findByRoom_Id(room.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No game state for this room"));
        Long currentGridId = state.getGridIds().get(state.getCurrentGridIndex());
        Grid grid = gridRepository.findById(currentGridId)
                .orElseThrow(() -> new ResourceNotFoundException("Grid no longer exists"));

        Set<Long> solvedEntryIds = solvedEntryRepository.findByRoomState_Id(state.getId()).stream()
                .map(GridBattleSolvedEntry::getGridEntryId).collect(Collectors.toSet());

        GridEntry matched = grid.getEntries().stream()
                .filter(e -> !solvedEntryIds.contains(e.getId()) && e.getAthlete().getId().equals(athleteId))
                .findFirst().orElse(null);

        GridBattleParticipantState myState = participantStateRepository
                .findByRoomState_IdAndParticipant_Id(state.getId(), me.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No progress state for you in this room"));

        if (matched != null) {
            GridBattleSolvedEntry se = new GridBattleSolvedEntry();
            se.setRoomState(state);
            se.setGridEntryId(matched.getId());
            se.setSolvedBy(me);
            solvedEntryRepository.save(se);
            myState.setTotalScore(myState.getTotalScore() + 1);
        } else {
            myState.setLivesUsedThisGrid(myState.getLivesUsedThisGrid() + 1);
        }
        participantStateRepository.save(myState);

        List<GameRoomParticipant> ordered = room.getParticipants();
        int myIndex = ordered.indexOf(me);
        state.setCurrentTurnParticipantIndex((myIndex + 1) % ordered.size());
        roomStateRepository.save(state);

        return getState(room, requestingEmail);
    }

    @Transactional
    public GridBattleStateDto advanceToNextGrid(GameRoom room, String requestingEmail) {
        if (!room.getHostEmail().equals(requestingEmail)) {
            throw new IllegalStateException("Only the host can move on to the next grid.");
        }
        GridBattleStateDto current = getState(room, requestingEmail);
        if (!current.isGridComplete()) {
            throw new IllegalStateException("This grid isn't finished yet.");
        }

        GridBattleRoomState state = roomStateRepository.findByRoom_Id(room.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No game state for this room"));

        solvedEntryRepository.deleteByRoomState_Id(state.getId());
        for (GridBattleParticipantState ps : participantStateRepository.findByRoomState_Id(state.getId())) {
            ps.setLivesUsedThisGrid(0);
            participantStateRepository.save(ps);
        }

        if (state.getCurrentGridIndex() + 1 >= state.getGridIds().size()) {
            state.setFinished(true);
            room.setStatus(RoomStatus.FINISHED);
            gameRoomRepository.save(room);
        } else {
            state.setCurrentGridIndex(state.getCurrentGridIndex() + 1);
            // rotate who starts each grid, same convention as Tension between questions
            state.setCurrentTurnParticipantIndex(state.getCurrentGridIndex() % room.getParticipants().size());
        }
        roomStateRepository.save(state);
        return getState(room, requestingEmail);
    }

    private List<GridBattlePlayerStateDto> toPlayerDtos(List<GridBattleParticipantState> states, Set<Long> eliminatedIds) {
        return states.stream().map(ps -> new GridBattlePlayerStateDto(
                ps.getParticipant().getId(), ps.getParticipant().getDisplayName(), ps.getParticipant().getColor(),
                roomService.isConnected(ps.getParticipant()), ps.getLivesUsedThisGrid(),
                eliminatedIds.contains(ps.getParticipant().getId()), ps.getTotalScore()
        )).collect(Collectors.toList());
    }

    private String hintColor(GridEntry entry) {
        return entry.getClub() != null ? entry.getClub().getColor() : null;
    }

    private String logoUrl(GridEntry entry) {
        return (entry.isShowLogo() && entry.getClub() != null) ? entry.getClub().getLogoUrl() : null;
    }

    private java.util.Comparator<GridEntry> entrySortOrderForOnline(Grid grid) {
        if (!grid.isRanked()) {
            return java.util.Comparator.comparingInt(GridEntry::getOrderIndex).thenComparing(GridEntry::getId);
        }
        java.util.Comparator<GridEntry> byValue = grid.isSortAscending()
                ? java.util.Comparator.comparingInt(GridEntry::getHintValue)
                : java.util.Comparator.comparingInt(GridEntry::getHintValue).reversed();
        return byValue.thenComparing(GridEntry::getId);
    }
}
