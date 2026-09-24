package com.quizapp.controller;

import com.quizapp.dto.CreateRoomRequest;
import com.quizapp.dto.JoinRoomRequest;
import com.quizapp.dto.RoomDto;
import com.quizapp.model.GameRoom;
import com.quizapp.model.GameRoomParticipant;
import com.quizapp.model.RoomGameType;
import com.quizapp.model.RoomStatus;
import com.quizapp.security.LoginRateLimiter;
import com.quizapp.service.BullseyeOnlineService;
import com.quizapp.service.FlashbackOnlineService;
import com.quizapp.service.GridBattleOnlineService;
import com.quizapp.service.FiveOhOneOnlineService;
import com.quizapp.service.ImposterOnlineService;
import com.quizapp.service.LineupBattleOnlineService;
import com.quizapp.service.PlayAccessService;
import com.quizapp.service.RoomBroadcastService;
import com.quizapp.service.RoomService;
import com.quizapp.service.TensionOnlineService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    private final RoomService roomService;
    private final GridBattleOnlineService gridBattleOnlineService;
    private final TensionOnlineService tensionOnlineService;
    private final ImposterOnlineService imposterOnlineService;
    private final FiveOhOneOnlineService fiveOhOneOnlineService;
    private final LineupBattleOnlineService lineupBattleOnlineService;
    private final BullseyeOnlineService bullseyeOnlineService;
    private final FlashbackOnlineService flashbackOnlineService;
    private final PlayAccessService playAccessService;
    private final RoomBroadcastService roomBroadcastService;
    private final LoginRateLimiter loginRateLimiter;

    public RoomController(RoomService roomService, GridBattleOnlineService gridBattleOnlineService,
                           TensionOnlineService tensionOnlineService, ImposterOnlineService imposterOnlineService,
                           FiveOhOneOnlineService fiveOhOneOnlineService,
                           LineupBattleOnlineService lineupBattleOnlineService,
                           BullseyeOnlineService bullseyeOnlineService,
                           FlashbackOnlineService flashbackOnlineService,
                           PlayAccessService playAccessService,
                           RoomBroadcastService roomBroadcastService,
                           LoginRateLimiter loginRateLimiter) {
        this.roomService = roomService;
        this.gridBattleOnlineService = gridBattleOnlineService;
        this.tensionOnlineService = tensionOnlineService;
        this.imposterOnlineService = imposterOnlineService;
        this.fiveOhOneOnlineService = fiveOhOneOnlineService;
        this.lineupBattleOnlineService = lineupBattleOnlineService;
        this.bullseyeOnlineService = bullseyeOnlineService;
        this.flashbackOnlineService = flashbackOnlineService;
        this.playAccessService = playAccessService;
        this.roomBroadcastService = roomBroadcastService;
        this.loginRateLimiter = loginRateLimiter;
    }

    @PostMapping
    public ResponseEntity<RoomDto> create(@Valid @RequestBody CreateRoomRequest request, Authentication authentication) {
        requireNotGuest(authentication);
        playAccessService.requireAccessForGameType(authentication, request.getGameType());
        String email = authentication.getName();
        GameRoom room = roomService.createRoomShell(request.getGameType(), email, request.getDisplayName(), request.getColor());

        if (request.getGameType() == RoomGameType.GRID_BATTLE) {
            gridBattleOnlineService.initializeGridSequence(room, request.getGridIds(), request.getRandomGridCount());
        } else if (request.getGameType() == RoomGameType.TENSION) {
            tensionOnlineService.initializeQuestionSequence(room, request.getTensionNumQuestions(),
                    request.getTensionCategory(), request.getTensionExcludeCategories());
        } else if (request.getGameType() == RoomGameType.IMPOSTER) {
            imposterOnlineService.initializeImposterSequence(room, request.getGridIds(), request.getRandomGridCount());
        } else if (request.getGameType() == RoomGameType.FIVE_O_ONE) {
            fiveOhOneOnlineService.initializeCategory(room, request.getFiveOhOneCategoryId());
        } else if (request.getGameType() == RoomGameType.STARTING_XI_BATTLE) {
            lineupBattleOnlineService.initializeLineupSequence(room, request.getLineupIds(), request.getRandomLineupCount());
        } else if (request.getGameType() == RoomGameType.FLASHBACK) {
            flashbackOnlineService.initializeYearSequence(room, request.getFlashbackNumRounds());
        }
        // BULLSEYE deliberately has no case here - its round count depends on the
        // final headcount, not known until start() below (see BullseyeOnlineService).

        return ResponseEntity.status(HttpStatus.CREATED).body(roomService.toDto(room, email));
    }

    @PostMapping("/{code}/join")
    public RoomDto join(@PathVariable String code, @Valid @RequestBody JoinRoomRequest request,
                         Authentication authentication, HttpServletRequest httpRequest) {
        // Room codes are only 5 characters - without this, a held GUEST token
        // could be used to brute-force codes with no throttle at all.
        String rateLimitKey = "room-join:" + clientKey(httpRequest);
        loginRateLimiter.checkAllowed(rateLimitKey);
        GameRoom existing;
        try {
            existing = roomService.findByCode(code);
        } catch (com.quizapp.exception.ResourceNotFoundException e) {
            loginRateLimiter.recordFailure(rateLimitKey);
            throw e;
        }
        // A guest has no AppUser row to check canPlayX flags against - the host
        // already passed that check when creating the room, so a guest joining it
        // is covered by that, not a separate check of their own (see isGuest).
        if (!isGuest(authentication)) {
            playAccessService.requireAccessForGameType(authentication, existing.getGameType());
        }
        String email = authentication.getName();
        GameRoom room = roomService.join(code, email, request.getDisplayName(), request.getColor());
        loginRateLimiter.recordSuccess(rateLimitKey);
        RoomDto dto = roomService.toDto(room, email);
        roomBroadcastService.broadcastLobby(code, dto);
        return dto;
    }

    /**
     * "Still here" ping - keeps GameRoomParticipant.lastSeenAt fresh (see
     * RoomService#isConnected) for a client that's now sitting on the
     * WebSocket push instead of polling GET /state, which used to be the only
     * thing that called RoomService#touch. Deliberately its own lightweight
     * endpoint rather than piggybacking on the state fetch: called on an
     * interval regardless of whether the socket or the fallback poll is
     * currently active, from useRoomChannel.js.
     */
    @PostMapping("/{code}/heartbeat")
    public void heartbeat(@PathVariable String code, Authentication authentication) {
        GameRoom room = roomService.findByCode(code);
        roomService.touch(roomService.requireParticipant(room, authentication.getName()));
    }

    @GetMapping("/{code}")
    public RoomDto get(@PathVariable String code, Authentication authentication) {
        GameRoom room = roomService.findByCode(code);
        return roomService.toDto(room, authentication.getName());
    }

    @PostMapping("/{code}/start")
    public RoomDto start(@PathVariable String code, Authentication authentication) {
        String email = authentication.getName();
        GameRoom room = roomService.findByCode(code);
        if (room.getGameType() == RoomGameType.GRID_BATTLE) {
            gridBattleOnlineService.startGame(room, email);
        } else if (room.getGameType() == RoomGameType.IMPOSTER) {
            imposterOnlineService.startGame(room, email);
        } else if (room.getGameType() == RoomGameType.FIVE_O_ONE) {
            fiveOhOneOnlineService.startGame(room, email);
        } else if (room.getGameType() == RoomGameType.STARTING_XI_BATTLE) {
            lineupBattleOnlineService.startGame(room, email);
        } else if (room.getGameType() == RoomGameType.BULLSEYE) {
            bullseyeOnlineService.startGame(room, email);
        } else if (room.getGameType() == RoomGameType.FLASHBACK) {
            flashbackOnlineService.startGame(room, email);
        } else {
            tensionOnlineService.startGame(room, email);
        }
        RoomDto dto = roomService.toDto(roomService.findByCode(code), email);
        // Lets every waiting lobby screen jump straight to the game the instant
        // the host starts it, instead of catching up on its next poll - each
        // client still fetches its own personalized starting state itself once
        // it mounts the game component, same as it always has.
        roomBroadcastService.broadcastLobby(code, dto);
        return dto;
    }

    /**
     * "Play again" - lets the host reuse this same room code/lobby/participant
     * list for another round instead of everyone leaving and re-sharing a
     * brand new code. Only valid once the previous round has actually
     * FINISHED (not mid-game, not still WAITING - see RoomStatus). Each
     * game's own restartForReplay tears down the finished round's state and
     * picks a fresh one of the same shape (round count, or - for 501 -  the
     * same category); Bullseye alone has nothing to re-pick here, since its
     * round count is only ever decided fresh once start() sees the final
     * roster. Once that's done the room drops back to WAITING, exactly like
     * right after create() - the host still has to hit "Start game" again
     * from the lobby, same as the first time.
     */
    @PostMapping("/{code}/restart")
    public RoomDto restart(@PathVariable String code, Authentication authentication) {
        String email = authentication.getName();
        GameRoom room = roomService.findByCode(code);
        if (!room.getHostEmail().equals(email)) {
            throw new IllegalStateException("Only the host can restart the game.");
        }
        if (room.getStatus() != RoomStatus.FINISHED) {
            throw new IllegalStateException("This room hasn't finished yet.");
        }
        if (room.getGameType() == RoomGameType.GRID_BATTLE) {
            gridBattleOnlineService.restartForReplay(room);
        } else if (room.getGameType() == RoomGameType.IMPOSTER) {
            imposterOnlineService.restartForReplay(room);
        } else if (room.getGameType() == RoomGameType.FIVE_O_ONE) {
            fiveOhOneOnlineService.restartForReplay(room);
        } else if (room.getGameType() == RoomGameType.STARTING_XI_BATTLE) {
            lineupBattleOnlineService.restartForReplay(room);
        } else if (room.getGameType() == RoomGameType.BULLSEYE) {
            bullseyeOnlineService.restartForReplay(room);
        } else if (room.getGameType() == RoomGameType.FLASHBACK) {
            flashbackOnlineService.restartForReplay(room);
        } else {
            tensionOnlineService.restartForReplay(room);
        }
        RoomDto dto = roomService.toDto(roomService.markWaitingForReplay(room), email);
        roomBroadcastService.broadcastLobby(code, dto);
        return dto;
    }

    /**
     * Host-only removal of a stuck/disconnected participant, mid-game or
     * still in the lobby - see the individual XxxOnlineService#kick methods
     * for why no turn-order patchup is needed afterward (every game already
     * recomputes "whose turn" fresh off the live participant list). Can't
     * target the host themselves - see claimHost for that path instead.
     */
    @PostMapping("/{code}/kick")
    public RoomDto kick(@PathVariable String code, @RequestParam Long participantId, Authentication authentication) {
        String email = authentication.getName();
        GameRoom room = roomService.findByCode(code);
        if (!room.getHostEmail().equals(email)) {
            throw new IllegalStateException("Only the host can remove a player.");
        }
        String targetEmail = room.getParticipants().stream()
                .filter(p -> p.getId().equals(participantId))
                .findFirst()
                .map(GameRoomParticipant::getUserEmail)
                .orElseThrow(() -> new IllegalStateException("That player isn't in this room."));
        if (room.getHostEmail().equals(targetEmail)) {
            throw new IllegalStateException("The host can't remove themselves - leave the room instead.");
        }

        if (room.getGameType() == RoomGameType.GRID_BATTLE) {
            gridBattleOnlineService.kick(room, participantId);
        } else if (room.getGameType() == RoomGameType.IMPOSTER) {
            imposterOnlineService.kick(room, participantId);
        } else if (room.getGameType() == RoomGameType.FIVE_O_ONE) {
            fiveOhOneOnlineService.kick(room, participantId);
        } else if (room.getGameType() == RoomGameType.STARTING_XI_BATTLE) {
            lineupBattleOnlineService.kick(room, participantId);
        } else if (room.getGameType() == RoomGameType.BULLSEYE) {
            bullseyeOnlineService.kick(room, participantId);
        } else if (room.getGameType() == RoomGameType.FLASHBACK) {
            flashbackOnlineService.kick(room, participantId);
        } else {
            tensionOnlineService.kick(room, participantId);
        }

        RoomDto dto = roomService.toDto(roomService.findByCode(code), email);
        roomBroadcastService.broadcastLobby(code, dto);
        return dto;
    }

    /**
     * Manual host takeover (see RoomService#claimHost) - any remaining
     * participant can claim host once the current host has actually gone
     * quiet (isConnected's 20s threshold), no auto-timer involved. Lets a
     * group recover from a host who disappeared mid-game without anyone
     * being able to restart, kick a stuck player, etc.
     */
    @PostMapping("/{code}/claim-host")
    public RoomDto claimHost(@PathVariable String code, Authentication authentication) {
        String email = authentication.getName();
        GameRoom room = roomService.claimHost(roomService.findByCode(code), email);
        RoomDto dto = roomService.toDto(room, email);
        roomBroadcastService.broadcastLobby(code, dto);
        return dto;
    }

    private boolean isGuest(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_GUEST"));
    }

    /** Only a real (Google or email/password) account can host - guests may only join. */
    private void requireNotGuest(Authentication authentication) {
        if (isGuest(authentication)) {
            throw new IllegalStateException("Guests can't host a game - sign in to create a room, or ask the host for a room code to join.");
        }
    }

    /** Same reasoning/shape as AuthController's own copy - Render sits behind a reverse proxy. */
    private String clientKey(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
