package com.quizapp.service;

import com.quizapp.dto.TensionAnswerEntryDto;
import com.quizapp.dto.TensionOnlineStateDto;
import com.quizapp.dto.TensionQuestionDto;
import com.quizapp.model.GameRoom;
import com.quizapp.model.GameRoomParticipant;
import com.quizapp.model.RoomGameType;
import com.quizapp.repository.GameRoomParticipantRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Covers the two new "stuck online game" recovery features: the host kicking
// a disconnected participant mid-game, and another player manually claiming
// host once the original host has gone quiet. See RoomController#kick /
// #claimHost and RoomService#claimHost/#removeParticipant.
@SpringBootTest
class RoomKickAndHostTransferTest {

    @Autowired
    private RoomService roomService;
    @Autowired
    private TensionOnlineService tensionOnlineService;
    @Autowired
    private TensionQuestionService tensionQuestionService;
    @Autowired
    private GameRoomParticipantRepository gameRoomParticipantRepository;

    private static final String HOST = "kick-host@example.com";
    private static final String GUEST = "kick-guest@example.com";
    private static final String THIRD = "kick-third@example.com";

    private void seedQuestions() {
        for (int i = 0; i < 2; i++) {
            TensionQuestionDto dto = new TensionQuestionDto();
            dto.setTitle("Kick test question " + System.nanoTime());
            dto.setMainCategory("KickTestCategory");
            TensionAnswerEntryDto a = new TensionAnswerEntryDto();
            a.setRank(1);
            a.setText("Answer " + System.nanoTime());
            dto.setSafeAnswers(List.of(a));
            tensionQuestionService.create(dto);
        }
    }

    // The exact scenario reported: a disconnected player leaves the room
    // stuck mid-round because everyone else is waiting on their turn/answer.
    // Kicking them must let the round - and the game - continue to
    // completion without throwing, with no manual turn-index patchup needed.
    @Test
    void kickingAStuckParticipantMidRoundLetsTheGameContinue() {
        seedQuestions();
        GameRoom room = roomService.createRoomShell(RoomGameType.TENSION, HOST, "Host", null);
        tensionOnlineService.initializeQuestionSequence(room, 2, "KickTestCategory", null);
        roomService.join(room.getRoomCode(), GUEST, "Guest", null);
        roomService.join(room.getRoomCode(), THIRD, "Third", null);
        room = roomService.findByCode(room.getRoomCode());
        tensionOnlineService.startGame(room, HOST);
        room = roomService.findByCode(room.getRoomCode());

        TensionOnlineStateDto state = tensionOnlineService.getState(room, HOST);
        assertThat(state.isFinished()).isFalse();
        Long stuckParticipantId = state.getCurrentTurnParticipantId();
        String stuckEmail = room.getParticipants().stream()
                .filter(p -> p.getId().equals(stuckParticipantId))
                .findFirst().orElseThrow().getUserEmail();

        // Kick whoever it currently is - proves this works regardless of
        // whether the stuck player happens to be the host's next-up or not.
        tensionOnlineService.kick(room, stuckParticipantId);
        room = roomService.findByCode(room.getRoomCode());

        assertThat(room.getParticipants()).noneMatch(p -> p.getId().equals(stuckParticipantId));
        assertThat(room.getParticipants()).hasSize(2);

        // The remaining two players can now finish the round without the
        // kicked player ever answering - this is the actual bug being fixed.
        List<String> remainingEmails = room.getParticipants().stream()
                .map(GameRoomParticipant::getUserEmail).filter(e -> !e.equals(stuckEmail)).toList();
        for (String email : remainingEmails) {
            GameRoom fresh = roomService.findByCode(room.getRoomCode());
            TensionOnlineStateDto current = tensionOnlineService.getState(fresh, email);
            if (!current.isRoundRevealed()) {
                tensionOnlineService.submitAnswer(fresh, email, "Some answer " + email);
            }
        }
        TensionOnlineStateDto afterAnswers = tensionOnlineService.getState(roomService.findByCode(room.getRoomCode()), HOST.equals(stuckEmail) ? remainingEmails.get(0) : HOST);
        assertThat(afterAnswers.isRoundRevealed()).isTrue();
    }

    @Test
    void hostCanRemoveAParticipantFromTheWaitingLobby() {
        GameRoom room = roomService.createRoomShell(RoomGameType.TENSION, HOST, "Host", null);
        roomService.join(room.getRoomCode(), GUEST, "Guest", null);
        room = roomService.findByCode(room.getRoomCode());
        Long guestId = room.getParticipants().stream()
                .filter(p -> p.getUserEmail().equals(GUEST)).findFirst().orElseThrow().getId();

        tensionOnlineService.kick(room, guestId);

        GameRoom after = roomService.findByCode(room.getRoomCode());
        assertThat(after.getParticipants()).hasSize(1);
        assertThat(after.getParticipants().get(0).getUserEmail()).isEqualTo(HOST);
    }

    @Test
    void claimHostSucceedsOnceTheCurrentHostHasGoneQuiet() {
        GameRoom room = roomService.createRoomShell(RoomGameType.TENSION, HOST, "Host", null);
        roomService.join(room.getRoomCode(), GUEST, "Guest", null);
        room = roomService.findByCode(room.getRoomCode());

        // Simulate the host going stale - push their lastSeenAt back past the
        // disconnect threshold, exactly what a real dropped connection looks
        // like server-side (see RoomService#isConnected).
        GameRoomParticipant hostParticipant = room.getParticipants().stream()
                .filter(p -> p.getUserEmail().equals(HOST)).findFirst().orElseThrow();
        hostParticipant.setLastSeenAt(Instant.now().minus(5, ChronoUnit.MINUTES));
        gameRoomParticipantRepository.save(hostParticipant);

        GameRoom updated = roomService.claimHost(roomService.findByCode(room.getRoomCode()), GUEST);
        assertThat(updated.getHostEmail()).isEqualTo(GUEST);
    }

    @Test
    void claimHostFailsWhileTheCurrentHostIsStillConnected() {
        GameRoom room = roomService.createRoomShell(RoomGameType.TENSION, HOST, "Host", null);
        roomService.join(room.getRoomCode(), GUEST, "Guest", null);
        GameRoom finalRoom = roomService.findByCode(room.getRoomCode());

        assertThatThrownBy(() -> roomService.claimHost(finalRoom, GUEST))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("still connected");
    }
}
