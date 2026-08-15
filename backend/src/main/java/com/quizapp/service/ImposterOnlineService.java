package com.quizapp.service;

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
    private final RoomService roomService;

    public ImposterOnlineService(GameRoomRepository gameRoomRepository,
                                  ImposterRoomStateRepository roomStateRepository,
                                  ImposterParticipantStateRepository participantStateRepository,
                                  ImposterFlippedTileRepository flippedTileRepository,
                                  ImposterGridRepository imposterGridRepository,
                                  ImposterTileRepository imposterTileRepository,
                                  RoomService roomService) {
        this.gameRoomRepository = gameRoomRepository;
        this.roomStateRepository = roomStateRepository;
        this.participantStateRepository = participantStateRepository;
        this.flippedTileRepository = flippedTileRepository;
        this.imposterGridRepository = imposterGridRepository;
        this.imposterTileRepository = imposterTileRepository;
        this.roomService = roomService;
    }

    /** Board sequence is decided at room-creation time, before anyone else has joined. */
    @Transactional
    public void initializeImposterSequence(GameRoom room, List<Long> gridIds, Integer randomCount) {
        List<Long> sequence;
        if (gridIds != null && !gridIds.isEmpty()) {
            sequence = gridIds;
        } else {
            int count = (randomCount != null && randomCount >= 2 && randomCount <= 4) ? randomCount : 2;
            List<ImposterGrid> all = imposterGridRepository.findAll();
            Collections.shuffle(all);
            sequence = all.stream().limit(count).map(ImposterGrid::getId).collect(Collectors.toList());
        }
        if (sequence.size() < 2 || sequence.size() > 4) {
            throw new IllegalArgumentException("Choose 2-4 boards to play.");
        }
        ImposterRoomState state = new ImposterRoomState();
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

        dto.setTotalGrids(state.getGridIds().size());
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

        if (state.getCurrentGridIndex() + 1 >= state.getGridIds().size()) {
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
}
