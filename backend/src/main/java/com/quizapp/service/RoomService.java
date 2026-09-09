package com.quizapp.service;

import com.quizapp.dto.RoomDto;
import com.quizapp.dto.RoomParticipantDto;
import com.quizapp.exception.ResourceNotFoundException;
import com.quizapp.model.*;
import com.quizapp.repository.GameRoomParticipantRepository;
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
    private final GameRoomParticipantRepository gameRoomParticipantRepository;

    public RoomService(GameRoomRepository gameRoomRepository, GameRoomParticipantRepository gameRoomParticipantRepository) {
        this.gameRoomRepository = gameRoomRepository;
        this.gameRoomParticipantRepository = gameRoomParticipantRepository;
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
            int maxPlayers = switch (room.getGameType()) {
                case FIVE_O_ONE -> 2;
                case GRID_BATTLE, IMPOSTER, STARTING_XI_BATTLE -> 5;
                case BULLSEYE, FLASHBACK -> 6;
                default -> 4; // TENSION
            };
            if (room.getParticipants().size() >= maxPlayers) {
                throw new IllegalStateException("This room already has the maximum of " + maxPlayers + " players.");
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

    /**
     * Refreshes lastSeenAt for a participant - called from the heartbeat
     * endpoint and from every online game's getState().
     *
     * Deliberately re-fetches a managed reference by ID instead of trusting
     * the passed-in `participant` object. Every call site loads its GameRoom
     * (and this participant along with it) via a separate, already-committed
     * roomService.findByCode() call in the controller - by the time that
     * object reaches here it's detached from any persistence context, so
     * setting a field on it directly is a no-op Hibernate never sees, unless
     * something re-attaches it first. With Spring Boot's open-in-view left on
     * its default (true) - the case for local dev, since nothing here
     * overrides it - a single Hibernate session spans the whole request
     * regardless of transaction boundaries, which happens to keep the entity
     * attached and silently masks this. application-prod.properties turns
     * open-in-view off on purpose (see its own comment), so this bit for
     * real in production: every heartbeat and every poll's touch() were
     * updating a detached object, lastSeenAt never actually moved past join
     * time, and every participant looked disconnected within
     * DISCONNECT_THRESHOLD of joining - no matter how active they were.
     * getReferenceById avoids a real SELECT (a lazy proxy is all an
     * UPDATE-only write needs) while guaranteeing the entity Hibernate
     * flushes is the one this transaction actually manages.
     */
    @Transactional
    public void touch(GameRoomParticipant participant) {
        gameRoomParticipantRepository.getReferenceById(participant.getId()).setLastSeenAt(Instant.now());
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
