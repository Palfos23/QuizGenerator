package com.quizapp.controller;

import com.quizapp.dto.CreateRoomRequest;
import com.quizapp.dto.JoinRoomRequest;
import com.quizapp.dto.RoomDto;
import com.quizapp.model.GameRoom;
import com.quizapp.model.RoomGameType;
import com.quizapp.service.BullseyeOnlineService;
import com.quizapp.service.FlashbackOnlineService;
import com.quizapp.service.GridBattleOnlineService;
import com.quizapp.service.FiveOhOneOnlineService;
import com.quizapp.service.ImposterOnlineService;
import com.quizapp.service.LineupBattleOnlineService;
import com.quizapp.service.PlayAccessService;
import com.quizapp.service.RoomService;
import com.quizapp.service.TensionOnlineService;
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

    public RoomController(RoomService roomService, GridBattleOnlineService gridBattleOnlineService,
                           TensionOnlineService tensionOnlineService, ImposterOnlineService imposterOnlineService,
                           FiveOhOneOnlineService fiveOhOneOnlineService,
                           LineupBattleOnlineService lineupBattleOnlineService,
                           BullseyeOnlineService bullseyeOnlineService,
                           FlashbackOnlineService flashbackOnlineService,
                           PlayAccessService playAccessService) {
        this.roomService = roomService;
        this.gridBattleOnlineService = gridBattleOnlineService;
        this.tensionOnlineService = tensionOnlineService;
        this.imposterOnlineService = imposterOnlineService;
        this.fiveOhOneOnlineService = fiveOhOneOnlineService;
        this.lineupBattleOnlineService = lineupBattleOnlineService;
        this.bullseyeOnlineService = bullseyeOnlineService;
        this.flashbackOnlineService = flashbackOnlineService;
        this.playAccessService = playAccessService;
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
    public RoomDto join(@PathVariable String code, @RequestBody JoinRoomRequest request, Authentication authentication) {
        GameRoom existing = roomService.findByCode(code);
        // A guest has no AppUser row to check canPlayX flags against - the host
        // already passed that check when creating the room, so a guest joining it
        // is covered by that, not a separate check of their own (see isGuest).
        if (!isGuest(authentication)) {
            playAccessService.requireAccessForGameType(authentication, existing.getGameType());
        }
        String email = authentication.getName();
        GameRoom room = roomService.join(code, email, request.getDisplayName(), request.getColor());
        return roomService.toDto(room, email);
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
        return roomService.toDto(roomService.findByCode(code), email);
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
}
