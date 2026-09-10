package com.quizapp.service;

import com.quizapp.dto.*;
import com.quizapp.exception.ResourceNotFoundException;
import com.quizapp.model.*;
import com.quizapp.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Online (separate-device) Bullseye. Structurally close to TensionOnlineService
 * (poll for state, submit-then-lazily-resolve-on-read, host advances), with two
 * real differences: the active participant pool shrinks by one every round
 * (someone's eliminated each time, so turn rotation and "has everyone answered"
 * both operate over the CURRENT active list, never the room's full participant
 * list), and the round sequence isn't picked until the room actually starts,
 * since its length depends on the final headcount (see startGame).
 *
 * Trust model: same as pass-and-play's BullseyeGame.vue - the full answer key
 * (every entry's real stat value) ships with the round the moment it starts,
 * not gated behind reveal. That's what lets the answer-picker's search work
 * immediately, and it's a deliberate, already-accepted trade-off for this game
 * (see BullseyeRoundStateDto/BullseyePlayService). What's NOT trusted to the
 * client is the actual scoring/elimination outcome - submitAnswer only ever
 * stores the raw guessed name, and getState resolves who's eliminated
 * server-side by matching every guess against the real entries itself.
 */
@Service
public class BullseyeOnlineService {

    private final GameRoomRepository gameRoomRepository;
    private final BullseyeRoomStateRepository roomStateRepository;
    private final BullseyeParticipantStateRepository participantStateRepository;
    private final BullseyeRoundAnswerRepository roundAnswerRepository;
    private final BullseyeQuestionRepository bullseyeQuestionRepository;
    private final BullseyePlayService bullseyePlayService;
    private final RoomService roomService;
    private final GamePlayEventService gamePlayEventService;

    public BullseyeOnlineService(GameRoomRepository gameRoomRepository,
                                  BullseyeRoomStateRepository roomStateRepository,
                                  BullseyeParticipantStateRepository participantStateRepository,
                                  BullseyeRoundAnswerRepository roundAnswerRepository,
                                  BullseyeQuestionRepository bullseyeQuestionRepository,
                                  BullseyePlayService bullseyePlayService,
                                  RoomService roomService,
                                  GamePlayEventService gamePlayEventService) {
        this.gameRoomRepository = gameRoomRepository;
        this.roomStateRepository = roomStateRepository;
        this.participantStateRepository = participantStateRepository;
        this.roundAnswerRepository = roundAnswerRepository;
        this.bullseyeQuestionRepository = bullseyeQuestionRepository;
        this.bullseyePlayService = bullseyePlayService;
        this.roomService = roomService;
        this.gamePlayEventService = gamePlayEventService;
    }

    @Transactional
    public void startGame(GameRoom room, String requestingEmail) {
        if (!room.getHostEmail().equals(requestingEmail)) {
            throw new IllegalStateException("Only the host can start the game.");
        }
        int playerCount = room.getParticipants().size();
        if (playerCount < 2) {
            throw new IllegalStateException("Need at least 2 players to start.");
        }

        // Exactly one elimination per round, so a game with N players always
        // needs N-1 rounds - only known now that the roster's actually final.
        int roundCount = playerCount - 1;
        List<Long> eligible = new ArrayList<>(bullseyeQuestionRepository.findBattleEligibleIds());
        if (eligible.size() < roundCount) {
            throw new IllegalStateException(
                    "Not enough Bullseye questions available for " + playerCount + " players yet - ask an admin to add more.");
        }
        Collections.shuffle(eligible);
        List<Long> questionIds = eligible.stream().limit(roundCount).collect(Collectors.toList());

        BullseyeRoomState state = new BullseyeRoomState();
        state.setRoom(room);
        state.setQuestionIds(questionIds);
        roomStateRepository.save(state);

        for (GameRoomParticipant p : room.getParticipants()) {
            BullseyeParticipantState ps = new BullseyeParticipantState();
            ps.setRoomState(state);
            ps.setParticipant(p);
            participantStateRepository.save(ps);
        }
        room.setStatus(RoomStatus.IN_PROGRESS);
        gameRoomRepository.save(room);
    }

    @Transactional
    public BullseyeOnlineStateDto getState(GameRoom room, String requestingEmail) {
        GameRoomParticipant me = roomService.requireParticipant(room, requestingEmail);
        roomService.touch(me);

        BullseyeOnlineStateDto dto = new BullseyeOnlineStateDto();
        dto.setRoomCode(room.getRoomCode());
        dto.setStatus(room.getStatus());
        dto.setYourParticipantId(me.getId());

        if (room.getStatus() == RoomStatus.WAITING) {
            return dto;
        }

        BullseyeRoomState state = roomStateRepository.findByRoom_Id(room.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No game state for this room"));
        List<BullseyeParticipantState> participantStates = participantStateRepository.findByRoomState_Id(state.getId());

        dto.setTotalQuestions(state.getQuestionIds().size());
        dto.setCurrentQuestionIndex(state.getCurrentQuestionIndex());
        dto.setFinished(state.isFinished());

        if (state.isFinished()) {
            dto.setPlayers(toPlayerDtos(participantStates, Collections.emptySet()));
            return dto;
        }

        Long currentQuestionId = state.getQuestionIds().get(state.getCurrentQuestionIndex());
        BullseyeRoundStateDto question = bullseyePlayService.getMultiplayerStartState(currentQuestionId);
        dto.setSport(question.getSport());
        dto.setTargetValue(question.getTargetValue());
        dto.setStatLabel(question.getStatLabel());
        dto.setGroupDigits(question.isGroupDigits());
        dto.setQuestionUpdatedAt(question.getUpdatedAt());
        dto.setEntries(question.getEntries());

        List<BullseyeParticipantState> active = participantStates.stream()
                .filter(ps -> !ps.isEliminated())
                .collect(Collectors.toList());
        List<GameRoomParticipant> activeOrdered = active.stream()
                .map(BullseyeParticipantState::getParticipant)
                .sorted(Comparator.comparingInt(GameRoomParticipant::getJoinOrder))
                .collect(Collectors.toList());

        List<BullseyeRoundAnswer> roundAnswers = roundAnswerRepository.findByRoomState_IdOrderByIdAsc(state.getId());
        Set<Long> answeredIds = roundAnswers.stream().map(a -> a.getParticipant().getId()).collect(Collectors.toSet());
        dto.setPlayers(toPlayerDtos(participantStates, answeredIds));
        dto.setAnswersSoFar(roundAnswers.stream()
                .map(a -> new BullseyeOnlineAnswerDto(a.getParticipant().getDisplayName(), a.getGuessedName()))
                .collect(Collectors.toList()));

        boolean allAnswered = answeredIds.size() >= activeOrdered.size();
        dto.setRoundRevealed(allAnswered);

        if (!allAnswered) {
            int idx = state.getCurrentTurnParticipantIndex();
            for (int tries = 0; tries < activeOrdered.size(); tries++) {
                GameRoomParticipant candidate = activeOrdered.get(idx % activeOrdered.size());
                if (!answeredIds.contains(candidate.getId())) {
                    dto.setCurrentTurnParticipantId(candidate.getId());
                    break;
                }
                idx++;
            }
            return dto;
        }

        // Everyone active has answered - resolve rankings/elimination against the
        // real entries. Recomputed on every poll once revealed (cheap, small
        // lists) so every device sees identical results without a race; only the
        // actual elimination write is guarded by roundResolved, below.
        Map<String, BullseyeEntryViewDto> entryByName = question.getEntries().stream()
                .collect(Collectors.toMap(e -> e.getAthleteName().toLowerCase(), e -> e, (a, b) -> a));

        List<Map.Entry<BullseyeRoundAnswer, Integer>> withDistance = new ArrayList<>();
        for (BullseyeRoundAnswer answer : roundAnswers) {
            BullseyeEntryViewDto matched = entryByName.get(answer.getGuessedName().trim().toLowerCase());
            int statValue = (matched != null && matched.getStatValue() != null) ? matched.getStatValue() : 0;
            withDistance.add(Map.entry(answer, Math.abs(statValue - question.getTargetValue())));
        }
        // Farthest first so the loser (index 0) and tie-break (earliest submission,
        // already guaranteed by findByRoomState_IdOrderByIdAsc's input order,
        // preserved by List.sort's stability) are both easy to read off.
        withDistance.sort((a, b) -> b.getValue() - a.getValue());
        BullseyeRoundAnswer eliminatedAnswer = withDistance.get(0).getKey();

        List<BullseyeOnlineResultDto> results = new ArrayList<>();
        for (Map.Entry<BullseyeRoundAnswer, Integer> entry : withDistance) {
            BullseyeRoundAnswer answer = entry.getKey();
            BullseyeEntryViewDto matched = entryByName.get(answer.getGuessedName().trim().toLowerCase());
            int statValue = (matched != null && matched.getStatValue() != null) ? matched.getStatValue() : 0;
            results.add(new BullseyeOnlineResultDto(
                    answer.getParticipant().getId(), answer.getParticipant().getDisplayName(), answer.getGuessedName(),
                    statValue, entry.getValue(), answer == eliminatedAnswer));
        }
        dto.setRoundResults(results);
        dto.setBullseyeAnswers(computeTruth(question, roundAnswers));

        if (!state.isRoundResolved()) {
            BullseyeParticipantState eliminatedState = participantStates.stream()
                    .filter(ps -> ps.getParticipant().getId().equals(eliminatedAnswer.getParticipant().getId()))
                    .findFirst().orElse(null);
            if (eliminatedState != null) {
                eliminatedState.setEliminated(true);
                eliminatedState.setEliminatedAtRound(state.getCurrentQuestionIndex() + 1);
                participantStateRepository.save(eliminatedState);
            }
            state.setRoundResolved(true);
            roomStateRepository.save(state);
            dto.setPlayers(toPlayerDtos(participantStateRepository.findByRoomState_Id(state.getId()), answeredIds));
        }

        return dto;
    }

    @Transactional
    public BullseyeOnlineStateDto submitAnswer(GameRoom room, String requestingEmail, String guessedName) {
        GameRoomParticipant me = roomService.requireParticipant(room, requestingEmail);
        BullseyeOnlineStateDto currentView = getState(room, requestingEmail);

        if (currentView.isFinished()) throw new IllegalStateException("This game has already finished.");
        if (currentView.isRoundRevealed()) throw new IllegalStateException("This round is already finished.");
        if (!me.getId().equals(currentView.getCurrentTurnParticipantId())) {
            throw new IllegalStateException("It's not your turn.");
        }

        String trimmed = guessedName.trim();
        boolean duplicate = currentView.getAnswersSoFar().stream()
                .anyMatch(a -> a.getGuessedName().equalsIgnoreCase(trimmed));
        if (duplicate) {
            throw new IllegalStateException("Someone's already guessed that name this round.");
        }

        BullseyeRoomState state = roomStateRepository.findByRoom_Id(room.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No game state for this room"));

        BullseyeRoundAnswer answer = new BullseyeRoundAnswer();
        answer.setRoomState(state);
        answer.setParticipant(me);
        answer.setGuessedName(trimmed);
        roundAnswerRepository.save(answer);

        List<BullseyeParticipantState> active = participantStateRepository.findByRoomState_Id(state.getId()).stream()
                .filter(ps -> !ps.isEliminated())
                .collect(Collectors.toList());
        int activeCount = active.size();
        state.setCurrentTurnParticipantIndex(state.getCurrentTurnParticipantIndex() + 1 < activeCount
                ? state.getCurrentTurnParticipantIndex() + 1 : 0);
        roomStateRepository.save(state);

        return getState(room, requestingEmail);
    }

    @Transactional
    public BullseyeOnlineStateDto nextRound(GameRoom room, String requestingEmail) {
        if (!room.getHostEmail().equals(requestingEmail)) {
            throw new IllegalStateException("Only the host can move on to the next round.");
        }
        BullseyeOnlineStateDto current = getState(room, requestingEmail);
        if (!current.isRoundRevealed()) {
            throw new IllegalStateException("Not everyone has answered yet.");
        }

        BullseyeRoomState state = roomStateRepository.findByRoom_Id(room.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No game state for this room"));

        roundAnswerRepository.deleteByRoomState_Id(state.getId());
        state.setRoundResolved(false);

        if (state.getCurrentQuestionIndex() + 1 >= state.getQuestionIds().size()) {
            state.setFinished(true);
            room.setStatus(RoomStatus.FINISHED);
            gameRoomRepository.save(room);
            gamePlayEventService.record(BattleGameType.BULLSEYE);
        } else {
            state.setCurrentQuestionIndex(state.getCurrentQuestionIndex() + 1);
            // Rotate who starts each round among whoever's still active, same
            // "roundIndex % activeCount" convention BullseyeGame.vue's
            // rotatedActivePlayers already uses for pass-and-play.
            long activeCount = participantStateRepository.findByRoomState_Id(state.getId()).stream()
                    .filter(ps -> !ps.isEliminated()).count();
            state.setCurrentTurnParticipantIndex(activeCount == 0 ? 0 : (int) (state.getCurrentQuestionIndex() % activeCount));
        }
        roomStateRepository.save(state);
        return getState(room, requestingEmail);
    }

    // The actual best possible answer(s) in the whole pool, annotated with who
    // (if anyone) guessed each one - mirrors BullseyeGame.vue's
    // bullseyeAnswersWithFoundState computed value, just built server-side here.
    private List<BullseyeOnlineTruthDto> computeTruth(BullseyeRoundStateDto question, List<BullseyeRoundAnswer> roundAnswers) {
        List<BullseyeEntryViewDto> valued = question.getEntries().stream()
                .filter(e -> e.getStatValue() != null)
                .collect(Collectors.toList());
        if (valued.isEmpty()) return Collections.emptyList();

        int minDistance = valued.stream()
                .mapToInt(e -> Math.abs(e.getStatValue() - question.getTargetValue()))
                .min().orElse(0);

        List<BullseyeOnlineTruthDto> out = new ArrayList<>();
        for (BullseyeEntryViewDto e : valued) {
            if (Math.abs(e.getStatValue() - question.getTargetValue()) != minDistance) continue;
            String foundBy = roundAnswers.stream()
                    .filter(a -> a.getGuessedName().trim().equalsIgnoreCase(e.getAthleteName()))
                    .map(a -> a.getParticipant().getDisplayName())
                    .findFirst().orElse(null);
            out.add(new BullseyeOnlineTruthDto(e.getAthleteId(), e.getAthleteName(), e.getStatValue(), foundBy));
        }
        return out;
    }

    private List<BullseyeOnlinePlayerDto> toPlayerDtos(List<BullseyeParticipantState> states, Set<Long> answeredIds) {
        return states.stream().map(ps -> new BullseyeOnlinePlayerDto(
                ps.getParticipant().getId(), ps.getParticipant().getDisplayName(), ps.getParticipant().getColor(),
                roomService.isConnected(ps.getParticipant()), answeredIds.contains(ps.getParticipant().getId()),
                ps.isEliminated(), ps.getEliminatedAtRound()
        )).collect(Collectors.toList());
    }
}
