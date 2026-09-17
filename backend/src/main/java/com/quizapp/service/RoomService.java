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
import java.util.Optional;
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
            String chosenName = (displayName != null && !displayName.isBlank()) ? displayName.trim() : userEmail;

            // Reclaim: a DISCONNECTED existing participant with the same name
            // (case-insensitive) hands their seat to this new identity instead
            // of being rejected or duplicated. This is what actually lets
            // someone back in after losing their session - most commonly a
            // guest, whose identity is a one-time token with no way back
            // otherwise, but it helps a signed-in user switching devices too.
            // Safe under this app's existing trust model: the room CODE is
            // already the only thing that gates joining at all, in the lobby
            // or mid-game; this doesn't add a new way in, just a way back to
            // a seat that's sitting idle. A still-CONNECTED participant is
            // never reclaimable - only an abandoned seat can be taken over.
            Optional<GameRoomParticipant> reclaimable = room.getParticipants().stream()
                    .filter(p -> p.getDisplayName().equalsIgnoreCase(chosenName))
                    .filter(p -> !isConnected(p))
                    .findFirst();
            if (reclaimable.isPresent()) {
                GameRoomParticipant participant = reclaimable.get();
                participant.setUserEmail(userEmail);
                if (color != null) {
                    participant.setColor(color);
                }
                participant.setLastSeenAt(Instant.now());
                return gameRoomRepository.save(room);
            }

            if (room.getStatus() != RoomStatus.WAITING) {
                throw new IllegalStateException("This game has already started - you can't join mid-game.");
            }
            int maxPlayers = switch (room.getGameType()) {
                case FIVE_O_ONE -> 2;
                case GRID_BATTLE, IMPOSTER, STARTING_XI_BATTLE, FLASHBACK -> 8;
                case BULLSEYE -> 10;
                default -> 10; // TENSION
            };
            if (room.getParticipants().size() >= maxPlayers) {
                throw new IllegalStateException("This room already has the maximum of " + maxPlayers + " players.");
            }
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

    /**
     * "Play again" (see RoomController#restart): puts an already-FINISHED
     * room's status back to WAITING so the same room code/lobby/participant
     * list can be reused for another round, instead of everyone having to
     * leave and re-share a brand new code. The game-specific round state
     * itself is torn down and re-initialized separately, by each
     * Xxx OnlineService#restartForReplay - this just flips the room's own
     * status once that's done.
     */
    @Transactional
    public GameRoom markWaitingForReplay(GameRoom room) {
        room.setStatus(RoomStatus.WAITING);
        return gameRoomRepository.save(room);
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
                .map(p -> new RoomParticipantDto(p.getId(), p.getDisplayName(), p.getColor(), isConnected(p),
                        p.getUserEmail().equals(room.getHostEmail())))
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

    // Manual host takeover (see RoomController#claimHost) - only when the
    // current host has actually gone quiet (isConnected, same 20s threshold
    // used everywhere else), not just "someone else wants to be host". No
    // auto-timer transfers this on its own; a remaining player has to
    // explicitly claim it once the old host is confirmed unreachable.
    @Transactional
    public GameRoom claimHost(GameRoom room, String requestingEmail) {
        GameRoomParticipant me = requireParticipant(room, requestingEmail);
        if (room.getHostEmail().equals(requestingEmail)) {
            throw new IllegalStateException("You're already the host.");
        }
        GameRoomParticipant currentHost = room.getParticipants().stream()
                .filter(p -> p.getUserEmail().equals(room.getHostEmail()))
                .findFirst()
                .orElse(null);
        if (currentHost != null && isConnected(currentHost)) {
            throw new IllegalStateException("The host is still connected.");
        }
        room.setHostEmail(me.getUserEmail());
        return gameRoomRepository.save(room);
    }

    // Removes a participant outright - used by each game's own kick() after
    // it's torn down that participant's game-specific state rows (answers,
    // solved entries, etc.), which must happen first or this throws the exact
    // ConstraintViolationException RoomCleanupService was fixed for: those
    // child rows still reference game_room_participants until deleted.
    // cascade=ALL/orphanRemoval=true on GameRoom.participants means removing
    // it from this collection (not calling the repository directly) is what
    // actually deletes the row.
    @Transactional
    public GameRoom removeParticipant(GameRoom room, Long participantId) {
        boolean removed = room.getParticipants().removeIf(p -> p.getId().equals(participantId));
        if (!removed) {
            throw new ResourceNotFoundException("No participant found with id " + participantId);
        }
        return gameRoomRepository.save(room);
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
