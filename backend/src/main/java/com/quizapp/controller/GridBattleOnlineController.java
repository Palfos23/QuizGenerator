package com.quizapp.controller;

import com.quizapp.dto.GridBattleGuessRequest;
import com.quizapp.dto.GridBattleStateDto;
import com.quizapp.model.GameRoom;
import com.quizapp.service.GridBattleOnlineService;
import com.quizapp.service.RoomService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rooms/{code}/grid-battle")
public class GridBattleOnlineController {

    private final RoomService roomService;
    private final GridBattleOnlineService gridBattleOnlineService;

    public GridBattleOnlineController(RoomService roomService, GridBattleOnlineService gridBattleOnlineService) {
        this.roomService = roomService;
        this.gridBattleOnlineService = gridBattleOnlineService;
    }

    /** Polled repeatedly by every participant's client - this is how state stays in sync. */
    @GetMapping("/state")
    public GridBattleStateDto state(@PathVariable String code, Authentication authentication) {
        GameRoom room = roomService.findByCode(code);
        return gridBattleOnlineService.getState(room, authentication.getName());
    }

    @PostMapping("/guess")
    public GridBattleStateDto guess(@PathVariable String code, @Valid @RequestBody GridBattleGuessRequest request,
                                     Authentication authentication) {
        GameRoom room = roomService.findByCode(code);
        return gridBattleOnlineService.guess(room, authentication.getName(), request.getAthleteId());
    }

    @PostMapping("/next-grid")
    public GridBattleStateDto nextGrid(@PathVariable String code, Authentication authentication) {
        GameRoom room = roomService.findByCode(code);
        return gridBattleOnlineService.advanceToNextGrid(room, authentication.getName());
    }
}
