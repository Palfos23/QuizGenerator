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
 * Online (separate-device) Flashback. Everyone active always guesses every
 * round (no elimination, unlike Bullseye), so turn rotation runs over the
 * room's full, never-shrinking participant list - closest to
 * TensionOnlineService in shape. The one real wrinkle: a hint's guessing pass
 * doesn't always end the round the way a Tension/Bullseye round always does
 * once everyone's answered - if nobody's exact and hints remain, it silently
 * advances to the next hint instead (see getState's loop) and only actually
 * "reveals" once someone's exact or the last hint's been reached, mirroring
 * FlashbackGame.vue's pass-and-play advanceTurn exactly.
 *
 * Trust model: unlike Bullseye, the target year is genuinely withheld from
 * every client until the round resolves - there's no autocomplete/search
 * that would need it early the way Bullseye's answer-picker does, so there's
 * no reason to trade that secrecy away for online play the way Bullseye does.
 */
@Service
public class FlashbackOnlineService {

    private static final int DEFAULT_ROUNDS = 5;
    private static final int MIN_ROUNDS = 1;
    private static final int MAX_ROUNDS = 10;

    private final GameRoomRepository gameRoomRepository;
    private final FlashbackRoomStateRepository roomStateRepository;
    private final FlashbackParticipantStateRepository participantStateRepository;
    private final FlashbackRoundGuessRepository roundGuessRepository;
    private final FlashbackYearRepository flashbackYearRepository;
    private final RoomService roomService;
    private final GamePlayEventService gamePlayEventService;

    public FlashbackOnlineService(GameRoomRepository gameRoomRepository,
                                   FlashbackRoomStateRepository roomStateRepository,
                                   FlashbackParticipantStateRepository participantStateRepository,
                                   FlashbackRoundGuessRepository roundGuessRepository,
                                   FlashbackYearRepository flashbackYearRepository,
                                   RoomService roomService,
                                   GamePlayEventService gamePlayEventService) {
        this.gameRoomRepository = gameRoomRepository;
        this.roomStateRepository = roomStateRepository;
        this.participantStateRepository = participantStateRepository;
        this.roundGuessRepository = roundGuessRepository;
        this.flashbackYearRepository = flashbackYearRepository;
        this.roomService = roomService;
        this.gamePlayEventService = gamePlayEventService;
    }

    @Transactional
    public void initializeYearSequence(GameRoom room, Integer numRounds) {
        int count = (numRounds != null && numRounds >= MIN_ROUNDS && numRounds <= MAX_ROUNDS) ? numRounds : DEFAULT_ROUNDS;
        List<Long> eligible = new ArrayList<>(flashbackYearRepository.findEligibleIds());
        if (eligible.size() < count) {
            throw new IllegalArgumentException("Not enough Flashback years available yet - ask an admin to add more.");
        }
        Collections.shuffle(eligible);
        FlashbackRoomState state = new FlashbackRoomState();
        state.setRoom(room);
        state.setYearIds(eligible.stream().limit(count).collect(Collectors.toList()));
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
        FlashbackRoomState state = roomStateRepository.findByRoom_Id(room.getId())
                .orElseThrow(() -> new IllegalStateException("This room isn't set up yet."));

        for (GameRoomParticipant p : room.getParticipants()) {
            FlashbackParticipantState ps = new FlashbackParticipantState();
            ps.setRoomState(state);
            ps.setParticipant(p);
            participantStateRepository.save(ps);
        }
        room.setStatus(RoomStatus.IN_PROGRESS);
        gameRoomRepository.save(room);
    }

    @Transactional
    public FlashbackOnlineStateDto getState(GameRoom room, String requestingEmail) {
        GameRoomParticipant me = roomService.requireParticipant(room, requestingEmail);
        roomService.touch(me);

        FlashbackOnlineStateDto dto = new FlashbackOnlineStateDto();
        dto.setRoomCode(room.getRoomCode());
        dto.setStatus(room.getStatus());
        dto.setYourParticipantId(me.getId());

        if (room.getStatus() == RoomStatus.WAITING) {
            return dto;
        }

        FlashbackRoomState state = roomStateRepository.findByRoom_Id(room.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No game state for this room"));
        List<FlashbackParticipantState> participantStates = participantStateRepository.findByRoomState_Id(state.getId());

        dto.setTotalRounds(state.getYearIds().size());
        dto.setCurrentRoundIndex(state.getCurrentRoundIndex());
        dto.setFinished(state.isFinished());

        if (state.isFinished()) {
            dto.setPlayers(toPlayerDtos(participantStates));
            return dto;
        }

        FlashbackYear year = flashbackYearRepository.findById(state.getYearIds().get(state.getCurrentRoundIndex()))
                .orElseThrow(() -> new ResourceNotFoundException("No year found for this round"));
        List<String> allHints = year.getHints();

        List<GameRoomParticipant> ordered = new ArrayList<>(room.getParticipants());
        ordered.sort(Comparator.comparingInt(GameRoomParticipant::getJoinOrder));

        // A hint's guessing pass can resolve into "everyone answered, but
        // nobody's exact and hints remain" - that's not a reveal, just a
        // silent move to the next hint (see class comment), so this loops
        // rather than returning after a single pass.
        while (true) {
            dto.setHintIndex(state.getCurrentHintIndex());
            dto.setVisibleHints(allHints.subList(0, state.getCurrentHintIndex() + 1));

            List<FlashbackRoundGuess> allGuessesThisRound = roundGuessRepository.findByRoomState_IdOrderByIdAsc(state.getId());
            dto.setGuesses(allGuessesThisRound.stream()
                    .map(g -> new FlashbackOnlineGuessDto(g.getParticipant().getDisplayName(), g.getGuessedYear(), g.getHintIndex()))
                    .collect(Collectors.toList()));

            List<FlashbackRoundGuess> thisHintGuesses = allGuessesThisRound.stream()
                    .filter(g -> g.getHintIndex() == state.getCurrentHintIndex())
                    .collect(Collectors.toList());
            Set<Long> answeredIds = thisHintGuesses.stream().map(g -> g.getParticipant().getId()).collect(Collectors.toSet());
            boolean allAnswered = answeredIds.size() >= ordered.size();

            if (!allAnswered) {
                dto.setRoundRevealed(false);
                int idx = state.getCurrentTurnParticipantIndex();
                for (int tries = 0; tries < ordered.size(); tries++) {
                    GameRoomParticipant candidate = ordered.get(idx % ordered.size());
                    if (!answeredIds.contains(candidate.getId())) {
                        dto.setCurrentTurnParticipantId(candidate.getId());
                        break;
                    }
                    idx++;
                }
                dto.setPlayers(toPlayerDtos(participantStates));
                return dto;
            }

            Map<Long, Integer> distanceByParticipant = new HashMap<>();
            for (FlashbackRoundGuess g : thisHintGuesses) {
                distanceByParticipant.put(g.getParticipant().getId(), Math.abs(g.getGuessedYear() - year.getYear()));
            }
            int minDistance = Collections.min(distanceByParticipant.values());

            if (minDistance > 0 && state.getCurrentHintIndex() < allHints.size() - 1) {
                // Nobody nailed it and there's more to reveal - move on, no
                // scoring yet. The next loop iteration re-checks against the
                // new (empty) set of guesses for the new hint.
                state.setCurrentHintIndex(state.getCurrentHintIndex() + 1);
                roomStateRepository.save(state);
                continue;
            }

            // Resolved - either an exact match, or the last hint's exhausted
            // with nobody exact, either way the round ends here. Ties (at
            // either distance 0 or the nonzero minimum) all share the point.
            List<String> winnerNames = thisHintGuesses.stream()
                    .filter(g -> distanceByParticipant.get(g.getParticipant().getId()) == minDistance)
                    .map(g -> g.getParticipant().getDisplayName())
                    .distinct()
                    .collect(Collectors.toList());
            int points = allHints.size() - state.getCurrentHintIndex();

            dto.setRoundRevealed(true);
            dto.setYear(year.getYear());
            dto.setRoundWinners(winnerNames);
            dto.setPointsAwarded(points);
            dto.setWasExactMatch(minDistance == 0);

            if (!state.isRoundResolved()) {
                Set<String> winnerSet = new HashSet<>(winnerNames);
                for (FlashbackParticipantState ps : participantStates) {
                    if (winnerSet.contains(ps.getParticipant().getDisplayName())) {
                        ps.setTotalScore(ps.getTotalScore() + points);
                        participantStateRepository.save(ps);
                    }
                }
                state.setRoundResolved(true);
                roomStateRepository.save(state);
                participantStates = participantStateRepository.findByRoomState_Id(state.getId());
            }
            dto.setPlayers(toPlayerDtos(participantStates));
            return dto;
        }
    }

    @Transactional
    public FlashbackOnlineStateDto submitGuess(GameRoom room, String requestingEmail, int guessedYear) {
        GameRoomParticipant me = roomService.requireParticipant(room, requestingEmail);
        FlashbackOnlineStateDto currentView = getState(room, requestingEmail);

        if (currentView.isFinished()) throw new IllegalStateException("This game has already finished.");
        if (currentView.isRoundRevealed()) throw new IllegalStateException("This round is already finished.");
        if (!me.getId().equals(currentView.getCurrentTurnParticipantId())) {
            throw new IllegalStateException("It's not your turn.");
        }

        // Only a year from an earlier, already-resolved hint is blocked as a
        // duplicate - a guess on the hint still in progress is never blocked,
        // since nothing's provably wrong yet until everyone's answered it
        // (that's exactly what lets two players legitimately tie). Same rule
        // as FlashbackGame.vue's pass-and-play submitCurrentGuess.
        boolean duplicate = currentView.getGuesses().stream()
                .anyMatch(g -> g.getYear() == guessedYear && g.getHintIndex() < currentView.getHintIndex());
        if (duplicate) {
            throw new IllegalStateException("That year's already been guessed on an earlier clue this round.");
        }

        FlashbackRoomState state = roomStateRepository.findByRoom_Id(room.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No game state for this room"));

        FlashbackRoundGuess guess = new FlashbackRoundGuess();
        guess.setRoomState(state);
        guess.setParticipant(me);
        guess.setGuessedYear(guessedYear);
        guess.setHintIndex(state.getCurrentHintIndex());
        roundGuessRepository.save(guess);

        int participantCount = room.getParticipants().size();
        state.setCurrentTurnParticipantIndex(state.getCurrentTurnParticipantIndex() + 1 < participantCount
                ? state.getCurrentTurnParticipantIndex() + 1 : 0);
        roomStateRepository.save(state);

        return getState(room, requestingEmail);
    }

    @Transactional
    public FlashbackOnlineStateDto nextRound(GameRoom room, String requestingEmail) {
        if (!room.getHostEmail().equals(requestingEmail)) {
            throw new IllegalStateException("Only the host can move on to the next round.");
        }
        FlashbackOnlineStateDto current = getState(room, requestingEmail);
        if (!current.isRoundRevealed()) {
            throw new IllegalStateException("This round isn't finished yet.");
        }

        FlashbackRoomState state = roomStateRepository.findByRoom_Id(room.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No game state for this room"));

        roundGuessRepository.deleteByRoomState_Id(state.getId());
        state.setRoundResolved(false);
        state.setCurrentHintIndex(0);

        if (state.getCurrentRoundIndex() + 1 >= state.getYearIds().size()) {
            state.setFinished(true);
            room.setStatus(RoomStatus.FINISHED);
            gameRoomRepository.save(room);
            gamePlayEventService.record(BattleGameType.FLASHBACK);
        } else {
            state.setCurrentRoundIndex(state.getCurrentRoundIndex() + 1);
            // Rotate who starts each round, same convention as Tension between
            // questions and FlashbackGame.vue's pass-and-play rotatedActivePlayers.
            state.setCurrentTurnParticipantIndex(state.getCurrentRoundIndex() % room.getParticipants().size());
        }
        roomStateRepository.save(state);
        return getState(room, requestingEmail);
    }

    private List<FlashbackOnlinePlayerDto> toPlayerDtos(List<FlashbackParticipantState> states) {
        return states.stream().map(ps -> new FlashbackOnlinePlayerDto(
                ps.getParticipant().getId(), ps.getParticipant().getDisplayName(), ps.getParticipant().getColor(),
                roomService.isConnected(ps.getParticipant()), ps.getTotalScore()
        )).collect(Collectors.toList());
    }
}
