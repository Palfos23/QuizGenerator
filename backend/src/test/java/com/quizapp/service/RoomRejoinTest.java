package com.quizapp.service;

import com.quizapp.model.GameRoom;
import com.quizapp.model.GameRoomParticipant;
import com.quizapp.model.RoomGameType;
import com.quizapp.model.RoomStatus;
import com.quizapp.repository.GameRoomParticipantRepository;
import com.quizapp.repository.GameRoomRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// A guest's identity is a one-time, never-reused token (see
// AuthService#loginAsGuest) - once it's gone (session lost, explicit logout,
// a fresh "Join as a guest" click), there's no way to prove "I'm the same
// person who was already in this room". RoomService#join now handles this by
// letting a new, unrecognized identity take over an existing DISCONNECTED
// participant's seat if the display name matches, rather than rejecting them
// outright (mid-game) or creating a confusing duplicate (in the lobby).
@SpringBootTest
class RoomRejoinTest {

    @Autowired
    private RoomService roomService;
    @Autowired
    private GameRoomRepository gameRoomRepository;
    @Autowired
    private GameRoomParticipantRepository gameRoomParticipantRepository;

    private static final String HOST = "rejoin-host@example.com";

    private GameRoomParticipant makeDisconnected(GameRoom room, String userEmail) {
        GameRoomParticipant p = room.getParticipants().stream()
                .filter(x -> x.getUserEmail().equals(userEmail)).findFirst().orElseThrow();
        p.setLastSeenAt(Instant.now().minus(5, ChronoUnit.MINUTES));
        return gameRoomParticipantRepository.save(p);
    }

    @Test
    void aNewIdentityWithTheSameNameReclaimsADisconnectedSeatMidGame() {
        GameRoom room = roomService.createRoomShell(RoomGameType.TENSION, HOST, "Host", null);
        roomService.join(room.getRoomCode(), "old-guest-1@example.com", "Alex", "#111111");
        room = roomService.findByCode(room.getRoomCode());
        Long originalParticipantId = room.getParticipants().stream()
                .filter(p -> p.getUserEmail().equals("old-guest-1@example.com")).findFirst().orElseThrow().getId();
        makeDisconnected(room, "old-guest-1@example.com");

        // Simulate the game having started, since that's the actual broken
        // scenario - a brand new identity could never get back in here before.
        room.setStatus(RoomStatus.IN_PROGRESS);
        gameRoomRepository.save(room);

        GameRoom updated = roomService.join(room.getRoomCode(), "new-guest-2@example.com", "Alex", "#222222");

        assertThat(updated.getParticipants()).hasSize(2); // host + the one reclaimed seat, not 3
        GameRoomParticipant reclaimed = updated.getParticipants().stream()
                .filter(p -> p.getDisplayName().equalsIgnoreCase("Alex")).findFirst().orElseThrow();
        assertThat(reclaimed.getId()).isEqualTo(originalParticipantId);
        assertThat(reclaimed.getUserEmail()).isEqualTo("new-guest-2@example.com");
        assertThat(reclaimed.getColor()).isEqualTo("#222222");
        assertThat(roomService.isConnected(reclaimed)).isTrue();
    }

    @Test
    void aStillConnectedParticipantsSeatCannotBeReclaimed() {
        GameRoom room = roomService.createRoomShell(RoomGameType.TENSION, HOST, "Host", null);
        roomService.join(room.getRoomCode(), "connected-guest@example.com", "Sam", "#111111");
        room = roomService.findByCode(room.getRoomCode());
        // Deliberately left connected (fresh lastSeenAt from creation) - a
        // new identity claiming the same name should be rejected as a normal
        // name collision, not silently take over someone's active seat.
        GameRoom finalRoom = room;

        assertThatThrownBy(() -> roomService.join(finalRoom.getRoomCode(), "impersonator@example.com", "Sam", null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already taken");
    }

    @Test
    void reclaimingInTheWaitingLobbyAlsoWorksNotJustMidGame() {
        GameRoom room = roomService.createRoomShell(RoomGameType.TENSION, HOST, "Host", null);
        roomService.join(room.getRoomCode(), "old-guest-3@example.com", "Robin", "#111111");
        room = roomService.findByCode(room.getRoomCode());
        makeDisconnected(room, "old-guest-3@example.com");
        // Room is still WAITING here - before this fix, the leftover
        // disconnected entry would have blocked a fresh "Robin" from
        // rejoining at all ("name already taken"), even in the lobby.

        GameRoom updated = roomService.join(room.getRoomCode(), "new-guest-4@example.com", "Robin", null);

        assertThat(updated.getParticipants()).hasSize(2);
        assertThat(updated.getParticipants().stream().filter(p -> p.getDisplayName().equalsIgnoreCase("Robin")).count())
                .isEqualTo(1);
    }
}
