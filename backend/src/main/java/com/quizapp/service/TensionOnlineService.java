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
public class TensionOnlineService {

    private final GameRoomRepository gameRoomRepository;
    private final TensionRoomStateRepository roomStateRepository;
    private final TensionParticipantStateRepository participantStateRepository;
    private final TensionRoundAnswerRepository roundAnswerRepository;
    private final TensionQuestionService tensionQuestionService;
    private final RoomService roomService;

    public TensionOnlineService(GameRoomRepository gameRoomRepository,
                                 TensionRoomStateRepository roomStateRepository,
                                 TensionParticipantStateRepository participantStateRepository,
                                 TensionRoundAnswerRepository roundAnswerRepository,
                                 TensionQuestionService tensionQuestionService,
                                 RoomService roomService) {
        this.gameRoomRepository = gameRoomRepository;
        this.roomStateRepository = roomStateRepository;
        this.participantStateRepository = participantStateRepository;
        this.roundAnswerRepository = roundAnswerRepository;
        this.tensionQuestionService = tensionQuestionService;
        this.roomService = roomService;
    }

    @Transactional
    public void initializeQuestionSequence(GameRoom room, Integer numQuestions, String category) {
        int count = (numQuestions != null && numQuestions >= 2 && numQuestions <= 10) ? numQuestions : 5;
        List<TensionQuestionDto> questions = tensionQuestionService.getRandom(count, category);
        if (questions.size() < 2) {
            throw new IllegalArgumentException("Not enough tension questions available for that category yet.");
        }
        TensionRoomState state = new TensionRoomState();
        state.setRoom(room);
        state.setQuestionIds(questions.stream().map(TensionQuestionDto::getId).collect(Collectors.toList()));
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
        TensionRoomState state = roomStateRepository.findByRoom_Id(room.getId())
                .orElseThrow(() -> new IllegalStateException("This room isn't set up yet."));

        for (GameRoomParticipant p : room.getParticipants()) {
            TensionParticipantState ps = new TensionParticipantState();
            ps.setRoomState(state);
            ps.setParticipant(p);
            participantStateRepository.save(ps);
        }
        room.setStatus(RoomStatus.IN_PROGRESS);
        gameRoomRepository.save(room);
    }

    @Transactional
    public TensionOnlineStateDto getState(GameRoom room, String requestingEmail) {
        GameRoomParticipant me = roomService.requireParticipant(room, requestingEmail);
        roomService.touch(me);

        TensionOnlineStateDto dto = new TensionOnlineStateDto();
        dto.setRoomCode(room.getRoomCode());
        dto.setStatus(room.getStatus());
        dto.setYourParticipantId(me.getId());

        if (room.getStatus() == RoomStatus.WAITING) {
            return dto;
        }

        TensionRoomState state = roomStateRepository.findByRoom_Id(room.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No game state for this room"));
        List<TensionParticipantState> participantStates = participantStateRepository.findByRoomState_Id(state.getId());

        dto.setTotalQuestions(state.getQuestionIds().size());
        dto.setCurrentQuestionIndex(state.getCurrentQuestionIndex());
        dto.setFinished(state.isFinished());

        if (state.isFinished()) {
            dto.setPlayers(toPlayerDtos(participantStates, Collections.emptySet()));
            return dto;
        }

        Long currentQuestionId = state.getQuestionIds().get(state.getCurrentQuestionIndex());
        TensionQuestionDto question = tensionQuestionService.getOne(currentQuestionId);
        dto.setQuestionTitle(question.getTitle());
        dto.setAnswersCategory(question.getAnswersCategory());
        dto.setTensionAnswerCount(question.getTensionAnswers().size());

        List<TensionRoundAnswer> roundAnswers = roundAnswerRepository.findByRoomState_Id(state.getId());
        Set<Long> answeredIds = roundAnswers.stream().map(a -> a.getParticipant().getId()).collect(Collectors.toSet());
        dto.setPlayers(toPlayerDtos(participantStates, answeredIds));
        dto.setAnswersSoFar(roundAnswers.stream()
                .map(a -> new TensionAnsweredSoFarDto(a.getParticipant().getDisplayName(), a.getAnswerText()))
                .collect(Collectors.toList()));

        boolean allAnswered = answeredIds.size() >= participantStates.size();
        dto.setRoundRevealed(allAnswered);

        if (!allAnswered) {
            List<GameRoomParticipant> ordered = room.getParticipants();
            int idx = state.getCurrentTurnParticipantIndex();
            for (int tries = 0; tries < ordered.size(); tries++) {
                GameRoomParticipant candidate = ordered.get(idx % ordered.size());
                if (!answeredIds.contains(candidate.getId())) {
                    dto.setCurrentTurnParticipantId(candidate.getId());
                    break;
                }
                idx++;
            }
        }

        if (allAnswered) {
            dto.setSafeAnswers(question.getSafeAnswers());
            dto.setTensionAnswers(question.getTensionAnswers());

            List<TensionRoundResultDto> results = new ArrayList<>();
            Map<String, TensionAnswerEntryDto> safeByText = question.getSafeAnswers().stream()
                    .collect(Collectors.toMap(a -> a.getText().toLowerCase(), a -> a, (a, b) -> a));
            Map<String, TensionAnswerEntryDto> tensionByText = question.getTensionAnswers().stream()
                    .collect(Collectors.toMap(a -> a.getText().toLowerCase(), a -> a, (a, b) -> a));

            Map<Long, Integer> scoreByParticipant = new HashMap<>();
            for (TensionRoundAnswer ra : roundAnswers) {
                String needle = ra.getAnswerText().trim().toLowerCase();
                int score;
                boolean matchedTension = false;
                if (safeByText.containsKey(needle)) {
                    score = safeByText.get(needle).getRank();
                } else if (tensionByText.containsKey(needle)) {
                    score = -5;
                    matchedTension = true;
                } else {
                    score = -3;
                }
                scoreByParticipant.put(ra.getParticipant().getId(), score);
                results.add(new TensionRoundResultDto(ra.getParticipant().getId(), ra.getParticipant().getDisplayName(),
                        ra.getAnswerText(), score, matchedTension));
            }
            dto.setRoundResults(results);

            if (!state.isRoundScored()) {
                for (TensionParticipantState ps : participantStates) {
                    Integer delta = scoreByParticipant.get(ps.getParticipant().getId());
                    if (delta != null) {
                        ps.setTotalScore(ps.getTotalScore() + delta);
                        participantStateRepository.save(ps);
                    }
                }
                state.setRoundScored(true);
                roomStateRepository.save(state);
                // reflect the just-applied totals in this same response
                dto.setPlayers(toPlayerDtos(participantStateRepository.findByRoomState_Id(state.getId()), answeredIds));
            }
        }

        return dto;
    }

    @Transactional
    public TensionOnlineStateDto submitAnswer(GameRoom room, String requestingEmail, String answerText) {
        GameRoomParticipant me = roomService.requireParticipant(room, requestingEmail);
        TensionOnlineStateDto currentView = getState(room, requestingEmail);

        if (currentView.isFinished()) throw new IllegalStateException("This game has already finished.");
        if (currentView.isRoundRevealed()) throw new IllegalStateException("This round is already finished.");
        if (!me.getId().equals(currentView.getCurrentTurnParticipantId())) {
            throw new IllegalStateException("It's not your turn.");
        }

        String trimmed = answerText.trim();
        boolean duplicate = currentView.getAnswersSoFar().stream()
                .anyMatch(a -> a.getAnswerText().equalsIgnoreCase(trimmed));
        if (duplicate) {
            throw new IllegalStateException("That answer's already been used by another player this round.");
        }

        TensionRoomState state = roomStateRepository.findByRoom_Id(room.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No game state for this room"));

        TensionRoundAnswer answer = new TensionRoundAnswer();
        answer.setRoomState(state);
        answer.setParticipant(me);
        answer.setAnswerText(trimmed);
        roundAnswerRepository.save(answer);

        List<GameRoomParticipant> ordered = room.getParticipants();
        int myIndex = ordered.indexOf(me);
        state.setCurrentTurnParticipantIndex((myIndex + 1) % ordered.size());
        roomStateRepository.save(state);

        return getState(room, requestingEmail);
    }

    @Transactional
    public TensionOnlineStateDto nextQuestion(GameRoom room, String requestingEmail) {
        TensionOnlineStateDto current = getState(room, requestingEmail);
        if (!current.isRoundRevealed()) {
            throw new IllegalStateException("Not everyone has answered yet.");
        }

        TensionRoomState state = roomStateRepository.findByRoom_Id(room.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No game state for this room"));

        roundAnswerRepository.deleteByRoomState_Id(state.getId());
        state.setRoundScored(false);

        if (state.getCurrentQuestionIndex() + 1 >= state.getQuestionIds().size()) {
            state.setFinished(true);
            room.setStatus(RoomStatus.FINISHED);
            gameRoomRepository.save(room);
        } else {
            state.setCurrentQuestionIndex(state.getCurrentQuestionIndex() + 1);
            state.setCurrentTurnParticipantIndex(state.getCurrentQuestionIndex() % room.getParticipants().size());
        }
        roomStateRepository.save(state);
        return getState(room, requestingEmail);
    }

    private List<TensionOnlinePlayerDto> toPlayerDtos(List<TensionParticipantState> states, Set<Long> answeredIds) {
        return states.stream().map(ps -> new TensionOnlinePlayerDto(
                ps.getParticipant().getId(), ps.getParticipant().getDisplayName(), ps.getParticipant().getColor(),
                roomService.isConnected(ps.getParticipant()), answeredIds.contains(ps.getParticipant().getId()),
                ps.getTotalScore()
        )).collect(Collectors.toList());
    }
}
