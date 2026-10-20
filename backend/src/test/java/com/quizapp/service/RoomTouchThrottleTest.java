package com.quizapp.service;

import com.quizapp.model.GameRoom;
import com.quizapp.model.GameRoomParticipant;
import com.quizapp.model.RoomGameType;
import com.quizapp.repository.GameRoomParticipantRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

// Every poll and heartbeat "touches" the participant; writing that to the database each time was a steady
// source of disk writes. A participant seen within the last few seconds is no longer rewritten.
@SpringBootTest
class RoomTouchThrottleTest {

    @Autowired private RoomService roomService;
    @Autowired private GameRoomParticipantRepository participantRepository;

    private GameRoomParticipant participantWithLastSeen(Instant lastSeen) {
        GameRoom room = roomService.createRoomShell(RoomGameType.TENSION, "touch-" + System.nanoTime() + "@example.com", "Host", null);
        GameRoomParticipant p = roomService.findByCode(room.getRoomCode()).getParticipants().get(0);
        p.setLastSeenAt(lastSeen);
        return participantRepository.save(p);
    }

    private Instant storedLastSeen(GameRoomParticipant p) {
        return participantRepository.findById(p.getId()).orElseThrow().getLastSeenAt();
    }

    @Test
    void aParticipantSeenAMomentAgoIsNotWrittenAgain() {
        Instant recent = Instant.now().minus(1, ChronoUnit.SECONDS);
        GameRoomParticipant p = participantWithLastSeen(recent);

        roomService.touch(p);

        assertThat(storedLastSeen(p)).isEqualTo(participantRepository.findById(p.getId()).orElseThrow().getLastSeenAt());
        assertThat(storedLastSeen(p)).isBefore(Instant.now().minus(500, ChronoUnit.MILLIS)); // still the old timestamp
    }

    @Test
    void aParticipantNotSeenForAWhileIsRefreshed() {
        Instant old = Instant.now().minus(30, ChronoUnit.SECONDS);
        GameRoomParticipant p = participantWithLastSeen(old);

        roomService.touch(p);

        assertThat(storedLastSeen(p)).isAfter(old.plus(20, ChronoUnit.SECONDS));
        assertThat(roomService.isConnected(participantRepository.findById(p.getId()).orElseThrow())).isTrue();
    }
}
