package com.quizapp.service;

import com.quizapp.dto.ImposterGridSummaryDto;
import com.quizapp.dto.ImposterOnlinePlayerStateDto;
import com.quizapp.dto.ImposterOnlineRevealDto;
import com.quizapp.dto.ImposterOnlineStateDto;
import com.quizapp.dto.ImposterOnlineTileDto;
import com.quizapp.exception.ResourceNotFoundException;
import com.quizapp.model.*;
import com.quizapp.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ImposterOnlineService {

    private final GameRoomRepository gameRoomRepository;
    private final ImposterRoomStateRepository roomStateRepository;
    private final ImposterParticipantStateRepository participantStateRepository;
    private final ImposterFlippedTileRepository flippedTileRepository;
    private final ImposterGridRepository imposterGridRepository;
    private final ImposterTileRepository imposterTileRepository;
    private final ImposterGridPlayService imposterGridPlayService;
    private final RoomService roomService;

    public ImposterOnlineService(GameRoomRepository gameRoomRepository,
                                  ImposterRoomStateRepository roomStateRepository,
                                  ImposterParticipantStateRepository participantStateRepository,
                                  ImposterFlippedTileRepository flippedTileRepository,
                                  ImposterGridRepository imposterGridRepository,
                                  ImposterTileRepository imposterTileRepository,
                                  ImposterGridPlayService imposterGridPlayService,
                                  RoomService roomService) {
        this.gameRoomRepository = gameRoomRepository;
        this.roomStateRepository = roomStateRepository;
        this.participantStateRepository = participantStateRepository;
        this.flippedTileRepository = flippedTileRepository;
        this.imposterGridRepository = imposterGridRepository;
        this.imposterTileRepository = imposterTileRepository;
        this.imposterGridPlayService = imposterGridPlayService;
        this.roomService = roomService;
    }

    /**
     * "Pick my own" decides the whole board sequence right here. "Random"
     * decides nothing yet - gridIds starts empty and grows one entry per
     * round, as that round's starting player picks from 3 live-generated
     * choices - see GridBattleOnlineService.initializeGridSequence for the
     * full reasoning this mirrors.
     */
    @Transactional
    public void initializeImposterSequence(GameRoom room, List<Long> gridIds, Integer randomCount) {
        ImposterRoomState state = new ImposterRoomState();
        state.setRoom(room);
        if (gridIds != null && !gridIds.isEmpty()) {
            if (gridIds.size() < 2 || gridIds.size() > 4) {
                throw new IllegalArgumentException("Choose 2-4 boards to play.");
            }
            state.setGridIds(gridIds);
        } else {
            int count = (randomCount != null && randomCount >= 2 && randomCount <= 4) ? randomCount : 2;
            state.setGridIds(new ArrayList<>());
            state.setRandomTotalCount(count);
        }
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
        ImposterRoomState state = roomStateRepository.findByRoom_Id(room.getId())
                .orElseThrow(() -> new IllegalStateException("This room isn't set up yet."));

        for (GameRoomParticipant p : room.getParticipants()) {
            ImposterParticipantState ps = new ImposterParticipantState();
            ps.setRoomState(state);
            ps.setParticipant(p);
            participantStateRepository.save(ps);
        }
        room.setStatus(RoomStatus.IN_PROGRESS);
        gameRoomRepository.save(room);
    }

    @Transactional
    public ImposterOnlineStateDto getState(GameRoom room, String requestingEmail) {
        GameRoomParticipant me = roomService.requireParticipant(room, requestingEmail);
        roomService.touch(me);

        ImposterOnlineStateDto dto = new ImposterOnlineStateDto();
        dto.setRoomCode(room.getRoomCode());
        dto.setStatus(room.getStatus());
        dto.setYourParticipantId(me.getId());

        if (room.getStatus() == RoomStatus.WAITING) {
            return dto;
        }

        ImposterRoomState state = roomStateRepository.findByRoom_Id(room.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No game state for this room"));
        List<ImposterParticipantState> participantStates = participantStateRepository.findByRoomState_Id(state.getId());

        dto.setTotalGrids(state.getRandomTotalCount() != null ? state.getRandomTotalCount() : state.getGridIds().size());
        dto.setCurrentGridIndex(state.getCurrentGridIndex());
        dto.setFinished(state.isFinished());

        List<GameRoomParticipant> connectedIds = room.getParticipants();
        dto.setPlayers(participantStates.stream()
                .map(ps -> new ImposterOnlinePlayerStateDto(
                        ps.getParticipant().getId(),
                        ps.getParticipant().getDisplayName(),
                        ps.getParticipant().getColor(),
                        connectedIds.contains(ps.getParticipant()),
                        ps.getTotalScore()))
                .collect(Collectors.toList()));

        if (state.isFinished()) {
            return dto;
        }

        // "Random" mode: this round's board hasn't been picked yet - offer 3
        // choices to whoever starts this round instead of anything else about
        // the round, which doesn't exist until they pick.
        if (state.getRandomTotalCount() != null && state.getGridIds().size() <= state.getCurrentGridIndex()) {
            ensurePendingChoices(state);
            dto.setAwaitingGridChoice(true);
            dto.setGridChoices(resolveChoiceSummaries(state.getPendingChoiceIds()));
            List<GameRoomParticipant> ordered = room.getParticipants();
            dto.setPickerParticipantId(ordered.get(state.getCurrentGridIndex() % ordered.size()).getId());
            return dto;
        }

        Long currentGridId = state.getGridIds().get(state.getCurrentGridIndex());
        ImposterGrid grid = imposterGridRepository.findById(currentGridId)
                .orElseThrow(() -> new ResourceNotFoundException("Imposter grid no longer exists"));
        dto.setCurrentGridId(currentGridId);
        dto.setGridTitle(grid.getTitle());
        dto.setGridDescription(grid.getDescription());
        dto.setDisplayMode(grid.getDisplayMode().name());

        List<ImposterFlippedTile> flips = flippedTileRepository.findByRoomState_Id(state.getId());
        Map<Long, ImposterFlippedTile> flipByTileId = flips.stream()
                .collect(Collectors.toMap(ImposterFlippedTile::getTileId, f -> f));

        List<ImposterTile> tiles = grid.getTiles().stream()
                .sorted((a, b) -> Integer.compare(a.getOrderIndex(), b.getOrderIndex()))
                .collect(Collectors.toList());
        dto.setImposterCount((int) tiles.stream().filter(ImposterTile::isImposter).count());

        dto.setTiles(tiles.stream()
                .map(t -> {
                    ImposterFlippedTile flip = flipByTileId.get(t.getId());
                    boolean flipped = flip != null;
                    return new ImposterOnlineTileDto(
                            t.getId(),
                            t.getAthlete().getName(),
                            flipped ? revealPhotoUrl(t) : resolvedPhotoUrl(t),
                            t.getClub() != null ? t.getClub().getLogoUrl() : null,
                            flipped,
                            flipped ? t.isImposter() : null,
                            flipped ? flip.getFlippedBy().getDisplayName() : null);
                })
                .collect(Collectors.toList()));

        int fitsFound = (int) tiles.stream()
                .filter(t -> flipByTileId.containsKey(t.getId()) && !t.isImposter())
                .count();
        int totalFits = tiles.size() - dto.getImposterCount();
        boolean allFlipped = flips.size() >= tiles.size();
        boolean onlyImpostersRemain = fitsFound == totalFits;
        dto.setBoardComplete(allFlipped || onlyImpostersRemain);

        if (!dto.isBoardComplete()) {
            List<GameRoomParticipant> ordered = room.getParticipants();
            int idx = state.getCurrentTurnParticipantIndex() % ordered.size();
            dto.setCurrentTurnParticipantId(ordered.get(idx).getId());
        }
        return dto;
    }

    // Same idea as ImposterGridPlayService.resolvedPhotoUrl - a tile-specific
    // photo choice if one was made, otherwise the athlete's own primary photo.
    private String resolvedPhotoUrl(ImposterTile t) {
        return t.getSelectedPhoto() != null ? t.getSelectedPhoto().getPhotoUrl() : t.getAthlete().getPhotoUrl();
    }

    // Same idea as ImposterGridPlayService.revealPhotoUrl - a dedicated reveal
    // photo for the outcome if the admin set one, otherwise whatever was
    // already showing before the flip.
    private String revealPhotoUrl(ImposterTile t) {
        if (t.isImposter()) {
            if (t.isRevealImposterUseDefaultPhoto()) return t.getAthlete().getPhotoUrl();
            if (t.getRevealImposterPhoto() != null) return t.getRevealImposterPhoto().getPhotoUrl();
        } else {
            if (t.isRevealCorrectUseDefaultPhoto()) return t.getAthlete().getPhotoUrl();
            if (t.getRevealCorrectPhoto() != null) return t.getRevealCorrectPhoto().getPhotoUrl();
        }
        return resolvedPhotoUrl(t);
    }

    @Transactional
    public ImposterOnlineStateDto flip(GameRoom room, String requestingEmail, Long tileId) {
        GameRoomParticipant me = roomService.requireParticipant(room, requestingEmail);
        ImposterOnlineStateDto currentView = getState(room, requestingEmail);

        if (currentView.isFinished()) throw new IllegalStateException("This game has already finished.");
        if (currentView.isBoardComplete()) throw new IllegalStateException("This board is already finished.");
        if (!me.getId().equals(currentView.getCurrentTurnParticipantId())) {
            throw new IllegalStateException("It's not your turn.");
        }

        ImposterRoomState state = roomStateRepository.findByRoom_Id(room.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No game state for this room"));

        boolean alreadyFlipped = flippedTileRepository.findByRoomState_Id(state.getId()).stream()
                .anyMatch(f -> f.getTileId().equals(tileId));
        if (alreadyFlipped) {
            throw new IllegalStateException("That tile has already been flipped.");
        }

        ImposterTile tile = imposterTileRepository.findById(tileId)
                .orElseThrow(() -> new ResourceNotFoundException("No tile found with id " + tileId));

        ImposterFlippedTile flip = new ImposterFlippedTile();
        flip.setRoomState(state);
        flip.setTileId(tileId);
        flip.setFlippedBy(me);
        flippedTileRepository.save(flip);

        if (tile.isImposter()) {
            ImposterParticipantState myState = participantStateRepository
                    .findByRoomState_IdAndParticipant_Id(state.getId(), me.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("No progress state for you in this room"));
            myState.setTotalScore(myState.getTotalScore() + 1);
            participantStateRepository.save(myState);
        }

        List<GameRoomParticipant> ordered = room.getParticipants();
        int myIndex = ordered.indexOf(me);
        state.setCurrentTurnParticipantIndex((myIndex + 1) % ordered.size());
        roomStateRepository.save(state);

        return getState(room, requestingEmail);
    }

    @Transactional(readOnly = true)
    public List<ImposterOnlineRevealDto> getReveal(GameRoom room, String requestingEmail) {
        roomService.requireParticipant(room, requestingEmail);
        ImposterOnlineStateDto currentView = getState(room, requestingEmail);
        if (!currentView.isBoardComplete()) {
            throw new IllegalStateException("This board isn't finished yet.");
        }

        ImposterRoomState state = roomStateRepository.findByRoom_Id(room.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No game state for this room"));
        Long currentGridId = state.getGridIds().get(state.getCurrentGridIndex());
        ImposterGrid grid = imposterGridRepository.findById(currentGridId)
                .orElseThrow(() -> new ResourceNotFoundException("Imposter grid no longer exists"));

        Map<Long, ImposterFlippedTile> flipByTileId = flippedTileRepository.findByRoomState_Id(state.getId()).stream()
                .collect(Collectors.toMap(ImposterFlippedTile::getTileId, f -> f));

        return grid.getTiles().stream()
                .filter(ImposterTile::isImposter)
                .sorted((a, b) -> Integer.compare(a.getOrderIndex(), b.getOrderIndex()))
                .map(t -> {
                    ImposterFlippedTile flip = flipByTileId.get(t.getId());
                    return new ImposterOnlineRevealDto(
                            t.getId(),
                            t.getAthlete().getName(),
                            t.getReplacedAthlete() != null ? t.getReplacedAthlete().getName() : null,
                            flip != null ? flip.getFlippedBy().getDisplayName() : null);
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public ImposterOnlineStateDto advanceToNextBoard(GameRoom room, String requestingEmail) {
        if (!room.getHostEmail().equals(requestingEmail)) {
            throw new IllegalStateException("Only the host can move on to the next board.");
        }
        ImposterOnlineStateDto current = getState(room, requestingEmail);
        if (!current.isBoardComplete()) {
            throw new IllegalStateException("This board isn't finished yet.");
        }

        ImposterRoomState state = roomStateRepository.findByRoom_Id(room.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No game state for this room"));

        flippedTileRepository.deleteByRoomState_Id(state.getId());

        // "Random" mode's gridIds only holds what's been chosen so far, not the
        // whole planned game - randomTotalCount is the real round count there.
        int totalGrids = state.getRandomTotalCount() != null ? state.getRandomTotalCount() : state.getGridIds().size();
        if (state.getCurrentGridIndex() + 1 >= totalGrids) {
            state.setFinished(true);
            room.setStatus(RoomStatus.FINISHED);
            gameRoomRepository.save(room);
        } else {
            state.setCurrentGridIndex(state.getCurrentGridIndex() + 1);
            // rotate who starts each board, same convention as grid battle between grids
            state.setCurrentTurnParticipantIndex(state.getCurrentGridIndex() % room.getParticipants().size());
        }
        roomStateRepository.save(state);
        return getState(room, requestingEmail);
    }

    /**
     * "Random" mode only: the current round's starting player picks one of the
     * 3 live-generated choices getState() offered - mirrors
     * GridBattleOnlineService.chooseGrid.
     */
    @Transactional
    public ImposterOnlineStateDto chooseGrid(GameRoom room, String requestingEmail, Long gridId) {
        GameRoomParticipant me = roomService.requireParticipant(room, requestingEmail);
        ImposterRoomState state = roomStateRepository.findByRoom_Id(room.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No game state for this room"));

        if (state.getRandomTotalCount() == null) {
            throw new IllegalStateException("This room isn't using random board selection.");
        }
        if (state.getGridIds().size() > state.getCurrentGridIndex()) {
            throw new IllegalStateException("A board has already been chosen for this round.");
        }
        List<GameRoomParticipant> ordered = room.getParticipants();
        GameRoomParticipant picker = ordered.get(state.getCurrentGridIndex() % ordered.size());
        if (!me.getId().equals(picker.getId())) {
            throw new IllegalStateException("It's not your turn to choose.");
        }
        if (!state.getPendingChoiceIds().contains(gridId)) {
            throw new IllegalArgumentException("That wasn't one of the offered choices.");
        }

        state.getGridIds().add(gridId);
        state.setPendingChoiceIds(new HashSet<>());
        state.setCurrentTurnParticipantIndex(state.getCurrentGridIndex() % ordered.size());
        roomStateRepository.save(state);

        return getState(room, requestingEmail);
    }

    private void ensurePendingChoices(ImposterRoomState state) {
        if (!state.getPendingChoiceIds().isEmpty()) return;
        List<ImposterGridSummaryDto> choices = imposterGridPlayService.getBattleRoundChoices(3, new ArrayList<>(state.getGridIds()));
        state.setPendingChoiceIds(choices.stream().map(ImposterGridSummaryDto::getId).collect(Collectors.toSet()));
        roomStateRepository.save(state);
    }

    private List<ImposterGridSummaryDto> resolveChoiceSummaries(Set<Long> ids) {
        if (ids.isEmpty()) return Collections.emptyList();
        return imposterGridPlayService.resolveSummaries(ids);
    }
}
