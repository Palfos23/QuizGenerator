package com.quizapp.service;

import com.quizapp.dto.RoomDto;
import com.quizapp.dto.RoomParticipantDto;
import com.quizapp.exception.ResourceNotFoundException;
import com.quizapp.model.*;
import com.quizapp.repository.GameRoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.stream.Collectors;

@Service
public class RoomService {

    // A poll older than this means the tab is probably closed/backgrounded/offline -
    // shown to other players as "disconnected" rather than treated as an error.
    private static final Duration DISCONNECT_THRESHOLD = Duration.ofSeconds(20);
    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // no 0/O/1/I - easy to misread aloud
    private static final SecureRandom RANDOM = new SecureRandom();

    private final GameRoomRepository gameRoomRepository;

    public RoomService(GameRoomRepository gameRoomRepository) {
        this.gameRoomRepository = gameRoomRepository;
    }

    @Transactional
    public GameRoom createRoomShell(RoomGameType gameType, String hostEmail, String displayName, String color) {
        GameRoom room = new GameRoom();
        room.setRoomCode(generateUniqueCode());
        room.setGameType(gameType);
        room.setHostEmail(hostEmail);
        room.setStatus(RoomStatus.WAITING);
        room = gameRoomRepository.save(room);
        addParticipant(room, hostEmail, displayName, color);
        return gameRoomRepository.save(room);
    }

    @Transactional
    public GameRoom join(String roomCode, String userEmail, String displayName, String color) {
        GameRoom room = findByCode(roomCode);
        boolean alreadyIn = room.getParticipants().stream().anyMatch(p -> p.getUserEmail().equals(userEmail));
        if (!alreadyIn) {
            if (room.getStatus() != RoomStatus.WAITING) {
                throw new IllegalStateException("This game has already started - you can't join mid-game.");
            }
            if (room.getParticipants().size() >= 4) {
                throw new IllegalStateException("This room already has the maximum of 4 players.");
            }
            String chosenName = (displayName != null && !displayName.isBlank()) ? displayName.trim() : userEmail;
            boolean nameTaken = room.getParticipants().stream()
                    .anyMatch(p -> p.getDisplayName().equalsIgnoreCase(chosenName));
            if (nameTaken) {
                throw new IllegalStateException(
                        "The name \"" + chosenName + "\" is already taken in this room - please use a different name.");
            }
            addParticipant(room, userEmail, chosenName, color);
        }
        return gameRoomRepository.save(room);
    }

    private void addParticipant(GameRoom room, String userEmail, String displayName, String color) {
        GameRoomParticipant participant = new GameRoomParticipant();
        participant.setRoom(room);
        participant.setUserEmail(userEmail);
        participant.setDisplayName(displayName != null && !displayName.isBlank() ? displayName : userEmail);
        participant.setColor(color != null ? color : "#4f46e5");
        participant.setJoinOrder(room.getParticipants().size());
        room.getParticipants().add(participant);
    }

    @Transactional(readOnly = true)
    public GameRoom findByCode(String roomCode) {
        return gameRoomRepository.findByRoomCode(roomCode.toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("No room found with code " + roomCode));
    }

    /** Called on every poll from a participant - doubles as a heartbeat, no separate endpoint needed. */
    @Transactional
    public void touch(GameRoomParticipant participant) {
        participant.setLastSeenAt(Instant.now());
    }

    public boolean isConnected(GameRoomParticipant participant) {
        return Duration.between(participant.getLastSeenAt(), Instant.now()).compareTo(DISCONNECT_THRESHOLD) < 0;
    }

    public RoomDto toDto(GameRoom room, String requestingUserEmail) {
        RoomDto dto = new RoomDto();
        dto.setId(room.getId());
        dto.setRoomCode(room.getRoomCode());
        dto.setGameType(room.getGameType());
        dto.setStatus(room.getStatus());
        dto.setHostEmail(room.getHostEmail());
        dto.setParticipants(room.getParticipants().stream()
                .map(p -> new RoomParticipantDto(p.getId(), p.getDisplayName(), p.getColor(), isConnected(p)))
                .collect(Collectors.toList()));
        room.getParticipants().stream()
                .filter(p -> p.getUserEmail().equals(requestingUserEmail))
                .findFirst()
                .ifPresent(p -> dto.setYourParticipantId(p.getId()));
        dto.setHost(room.getHostEmail().equals(requestingUserEmail));
        return dto;
    }

    public GameRoomParticipant requireParticipant(GameRoom room, String userEmail) {
        return room.getParticipants().stream()
                .filter(p -> p.getUserEmail().equals(userEmail))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("You're not a participant in this room."));
    }

    private String generateUniqueCode() {
        String code;
        do {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 5; i++) sb.append(CODE_CHARS.charAt(RANDOM.nextInt(CODE_CHARS.length())));
            code = sb.toString();
        } while (gameRoomRepository.findByRoomCode(code).isPresent());
        return code;
    }
}
