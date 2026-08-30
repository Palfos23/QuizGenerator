package com.quizapp.service;

import com.quizapp.dto.*;
import com.quizapp.exception.ResourceNotFoundException;
import com.quizapp.model.*;
import com.quizapp.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class FiveOhOneOnlineService {

    // Exact same set as FiveOhOneGame.vue's IMPOSSIBLE_CHECKOUTS - the nine
    // scores that can't be hit with three darts, so they score zero here too.
    private static final Set<Integer> IMPOSSIBLE_CHECKOUTS = Set.of(163, 166, 169, 172, 173, 175, 176, 178, 179);

    private final GameRoomRepository gameRoomRepository;
    private final FiveOhOneRoomStateRepository roomStateRepository;
    private final FiveOhOneParticipantStateRepository participantStateRepository;
    private final FiveOhOneThrowRepository throwRepository;
    private final FiveOhOneCategoryRepository categoryRepository;
    private final RoomService roomService;
    private final GamePlayEventService gamePlayEventService;

    public FiveOhOneOnlineService(GameRoomRepository gameRoomRepository,
                                   FiveOhOneRoomStateRepository roomStateRepository,
                                   FiveOhOneParticipantStateRepository participantStateRepository,
                                   FiveOhOneThrowRepository throwRepository,
                                   FiveOhOneCategoryRepository categoryRepository,
                                   RoomService roomService,
                                   GamePlayEventService gamePlayEventService) {
        this.gameRoomRepository = gameRoomRepository;
        this.roomStateRepository = roomStateRepository;
        this.participantStateRepository = participantStateRepository;
        this.throwRepository = throwRepository;
        this.categoryRepository = categoryRepository;
        this.roomService = roomService;
        this.gamePlayEventService = gamePlayEventService;
    }

    /** Category is decided at room-creation time, before anyone else has joined. */
    @Transactional
    public void initializeCategory(GameRoom room, Long categoryId) {
        if (categoryId == null || !categoryRepository.existsById(categoryId)) {
            throw new IllegalArgumentException("Choose a category to play.");
        }
        FiveOhOneRoomState state = new FiveOhOneRoomState();
        state.setRoom(room);
        state.setCategoryId(categoryId);
        roomStateRepository.save(state);
    }

    @Transactional
    public void startGame(GameRoom room, String requestingEmail) {
        if (!room.getHostEmail().equals(requestingEmail)) {
            throw new IllegalStateException("Only the host can start the game.");
        }
        if (room.getParticipants().size() != 2) {
            throw new IllegalStateException("501 needs exactly 2 players to start.");
        }
        FiveOhOneRoomState state = roomStateRepository.findByRoom_Id(room.getId())
                .orElseThrow(() -> new IllegalStateException("This room isn't set up yet."));

        for (GameRoomParticipant p : room.getParticipants()) {
            FiveOhOneParticipantState ps = new FiveOhOneParticipantState();
            ps.setRoomState(state);
            ps.setParticipant(p);
            participantStateRepository.save(ps);
        }
        room.setStatus(RoomStatus.IN_PROGRESS);
        gameRoomRepository.save(room);
    }

    private int effectiveScore(int rawValue) {
        if (rawValue > 180) return 0;
        if (IMPOSSIBLE_CHECKOUTS.contains(rawValue)) return 0;
        return rawValue;
    }

    @Transactional
    public FiveOhOneOnlineStateDto getState(GameRoom room, String requestingEmail) {
        GameRoomParticipant me = roomService.requireParticipant(room, requestingEmail);
        roomService.touch(me);

        FiveOhOneOnlineStateDto dto = new FiveOhOneOnlineStateDto();
        dto.setRoomCode(room.getRoomCode());
        dto.setStatus(room.getStatus());
        dto.setYourParticipantId(me.getId());

        if (room.getStatus() == RoomStatus.WAITING) {
            return dto;
        }

        FiveOhOneRoomState state = roomStateRepository.findByRoom_Id(room.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No game state for this room"));
        List<FiveOhOneParticipantState> participantStates = participantStateRepository.findByRoomState_Id(state.getId());

        List<GameRoomParticipant> connected = room.getParticipants();
        dto.setPlayers(participantStates.stream()
                .map(ps -> new FiveOhOneOnlinePlayerStateDto(
                        ps.getParticipant().getId(),
                        ps.getParticipant().getDisplayName(),
                        ps.getParticipant().getColor(),
                        connected.contains(ps.getParticipant()),
                        ps.getTotal()))
                .collect(Collectors.toList()));

        dto.setFinished(state.isFinished());
        dto.setWindowReacherParticipantId(state.getWindowReacherParticipantId());
        dto.setWinnerParticipantId(state.getWinnerParticipantId());

        FiveOhOneCategory category = categoryRepository.findById(state.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category no longer exists"));
        dto.setCategoryId(category.getId());
        dto.setCategoryTitle(category.getTitle());
        dto.setCategoryDescription(category.getDescription());
        dto.setCategoryUpdatedAt(category.getUpdatedAt());

        List<FiveOhOneThrow> allThrows = throwRepository.findByRoomState_IdOrderByIdAsc(state.getId());
        Set<Long> usedEntryIds = allThrows.stream().map(FiveOhOneThrow::getEntryId).collect(Collectors.toSet());
        dto.setUsedEntryIds(new ArrayList<>(usedEntryIds));
        dto.setThrowHistory(allThrows.stream()
                .map(t -> new FiveOhOneThrowDto(t.getThrownBy().getDisplayName(), t.getEntryName(), t.getRawValue(),
                        t.getScore(), t.isBust(), t.getResultingTotal()))
                .collect(Collectors.toList()));

        if (!state.isFinished()) {
            List<GameRoomParticipant> ordered = room.getParticipants();
            int idx = state.getCurrentTurnParticipantIndex() % ordered.size();
            Long currentTurnId = ordered.get(idx).getId();
            dto.setCurrentTurnParticipantId(currentTurnId);

            // Best-available-score / checkout-count are only meaningful for
            // whoever's turn it currently is - same as the pass-and-play version.
            int currentTotal = participantStates.stream()
                    .filter(ps -> ps.getParticipant().getId().equals(currentTurnId))
                    .findFirst().map(FiveOhOneParticipantState::getTotal).orElse(501);
            List<FiveOhOneEntry> unused = category.getEntries().stream()
                    .filter(e -> !usedEntryIds.contains(e.getId()))
                    .collect(Collectors.toList());

            if (currentTotal > 180) {
                dto.setBestAvailableScore(unused.isEmpty() ? 0 :
                        unused.stream().mapToInt(e -> effectiveScore(e.getValue())).max().orElse(0));
            } else {
                long count = unused.stream()
                        .filter(e -> {
                            int resulting = currentTotal - effectiveScore(e.getValue());
                            return resulting >= -10 && resulting <= 0;
                        })
                        .count();
                dto.setCheckoutCount((int) count);
            }
        }
        return dto;
    }

    @Transactional
    public FiveOhOneOnlineStateDto throwEntry(GameRoom room, String requestingEmail, Long entryId) {
        GameRoomParticipant me = roomService.requireParticipant(room, requestingEmail);
        FiveOhOneOnlineStateDto currentView = getState(room, requestingEmail);

        if (currentView.isFinished()) throw new IllegalStateException("This game has already finished.");
        if (!me.getId().equals(currentView.getCurrentTurnParticipantId())) {
            throw new IllegalStateException("It's not your turn.");
        }
        if (currentView.getUsedEntryIds().contains(entryId)) {
            throw new IllegalStateException("That name has already been thrown.");
        }

        FiveOhOneRoomState state = roomStateRepository.findByRoom_Id(room.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No game state for this room"));
        FiveOhOneEntry entry = categoryRepository.findById(state.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category no longer exists"))
                .getEntries().stream()
                .filter(e -> e.getId().equals(entryId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("No entry found with id " + entryId));

        FiveOhOneParticipantState myState = participantStateRepository
                .findByRoomState_IdAndParticipant_Id(state.getId(), me.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No progress state for you in this room"));

        int score = effectiveScore(entry.getValue());
        int previousTotal = myState.getTotal();
        int candidateTotal = previousTotal - score;
        boolean bust = candidateTotal < -10;
        int resultingTotal = bust ? previousTotal : candidateTotal;

        if (!bust) {
            myState.setTotal(candidateTotal);
            participantStateRepository.save(myState);
        }

        FiveOhOneThrow throwRecord = new FiveOhOneThrow();
        throwRecord.setRoomState(state);
        throwRecord.setThrownBy(me);
        throwRecord.setEntryId(entryId);
        throwRecord.setEntryName(entry.getName());
        throwRecord.setRawValue(entry.getValue());
        throwRecord.setScore(score);
        throwRecord.setBust(bust);
        throwRecord.setResultingTotal(resultingTotal);
        throwRepository.save(throwRecord);

        boolean landedInWindow = !bust && candidateTotal >= -10 && candidateTotal <= 0;
        Long windowReacherId = state.getWindowReacherParticipantId();

        if (windowReacherId != null && !windowReacherId.equals(me.getId())) {
            // this was the opponent's one response turn after someone reached the window
            if (landedInWindow) {
                FiveOhOneParticipantState otherState = participantStateRepository
                        .findByRoomState_IdAndParticipant_Id(state.getId(), windowReacherId)
                        .orElseThrow(() -> new ResourceNotFoundException("No progress state for the other player"));
                // closer to zero wins; a genuine tie favors whoever reached the window first
                state.setWinnerParticipantId(Math.abs(candidateTotal) <= Math.abs(otherState.getTotal())
                        ? me.getId() : windowReacherId);
            } else {
                state.setWinnerParticipantId(windowReacherId);
            }
            state.setFinished(true);
            roomStateRepository.save(state);
            gamePlayEventService.record(BattleGameType.FIVE_O_ONE);
            return getState(room, requestingEmail);
        }

        if (landedInWindow && windowReacherId == null) {
            state.setWindowReacherParticipantId(me.getId());
            List<GameRoomParticipant> ordered = room.getParticipants();
            if (ordered.size() == 2 && me.getId().equals(ordered.get(1).getId())) {
                // the second-starting player reached the window first - the first-starting
                // player doesn't get a response turn, the game ends right here
                state.setWinnerParticipantId(me.getId());
                state.setFinished(true);
                roomStateRepository.save(state);
                gamePlayEventService.record(BattleGameType.FIVE_O_ONE);
                return getState(room, requestingEmail);
            }
            // the first-starting player reached it - the second-starting player still
            // gets their natural next turn to respond, handled by the branch above
        }

        List<GameRoomParticipant> ordered = room.getParticipants();
        int myIndex = ordered.indexOf(me);
        state.setCurrentTurnParticipantIndex((myIndex + 1) % ordered.size());
        roomStateRepository.save(state);

        return getState(room, requestingEmail);
    }
}
