package com.quizapp.controller;

import com.quizapp.dto.CreateRoomRequest;
import com.quizapp.dto.JoinRoomRequest;
import com.quizapp.dto.RoomDto;
import com.quizapp.model.GameRoom;
import com.quizapp.model.RoomGameType;
import com.quizapp.service.GridBattleOnlineService;
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

    public RoomController(RoomService roomService, GridBattleOnlineService gridBattleOnlineService,
                           TensionOnlineService tensionOnlineService) {
        this.roomService = roomService;
        this.gridBattleOnlineService = gridBattleOnlineService;
        this.tensionOnlineService = tensionOnlineService;
    }

    @PostMapping
    public ResponseEntity<RoomDto> create(@Valid @RequestBody CreateRoomRequest request, Authentication authentication) {
        String email = authentication.getName();
        GameRoom room = roomService.createRoomShell(request.getGameType(), email, request.getDisplayName(), request.getColor());

        if (request.getGameType() == RoomGameType.GRID_BATTLE) {
            gridBattleOnlineService.initializeGridSequence(room, request.getGridIds(), request.getRandomGridCount());
        } else if (request.getGameType() == RoomGameType.TENSION) {
            tensionOnlineService.initializeQuestionSequence(room, request.getTensionNumQuestions(), request.getTensionCategory());
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(roomService.toDto(room, email));
    }

    @PostMapping("/{code}/join")
    public RoomDto join(@PathVariable String code, @RequestBody JoinRoomRequest request, Authentication authentication) {
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
        } else {
            tensionOnlineService.startGame(room, email);
        }
        return roomService.toDto(roomService.findByCode(code), email);
    }
}
