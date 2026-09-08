package com.quizapp.controller;

import com.quizapp.dto.GridBattleChooseRequest;
import com.quizapp.dto.GridBattleGuessRequest;
import com.quizapp.dto.GridBattleStateDto;
import com.quizapp.model.GameRoom;
import com.quizapp.service.GridBattleOnlineService;
import com.quizapp.service.RoomBroadcastService;
import com.quizapp.service.RoomService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rooms/{code}/grid-battle")
public class GridBattleOnlineController {

    private final RoomService roomService;
    private final GridBattleOnlineService gridBattleOnlineService;
    private final RoomBroadcastService roomBroadcastService;

    public GridBattleOnlineController(RoomService roomService, GridBattleOnlineService gridBattleOnlineService,
                                       RoomBroadcastService roomBroadcastService) {
        this.roomService = roomService;
        this.gridBattleOnlineService = gridBattleOnlineService;
        this.roomBroadcastService = roomBroadcastService;
    }

    /** Still exposed for the initial fetch on mount, and as this room's poll-based
     *  fallback if the WebSocket push below can't connect - see useRoomChannel.js. */
    @GetMapping("/state")
    public GridBattleStateDto state(@PathVariable String code, Authentication authentication) {
        GameRoom room = roomService.findByCode(code);
        return gridBattleOnlineService.getState(room, authentication.getName());
    }

    @PostMapping("/guess")
    public GridBattleStateDto guess(@PathVariable String code, @Valid @RequestBody GridBattleGuessRequest request,
                                     Authentication authentication) {
        GameRoom room = roomService.findByCode(code);
        GridBattleStateDto result = gridBattleOnlineService.guess(room, authentication.getName(), request.getAthleteId());
        roomBroadcastService.broadcastState(code, result);
        return result;
    }

    @PostMapping("/choose-grid")
    public GridBattleStateDto chooseGrid(@PathVariable String code, @Valid @RequestBody GridBattleChooseRequest request,
                                          Authentication authentication) {
        GameRoom room = roomService.findByCode(code);
        GridBattleStateDto result = gridBattleOnlineService.chooseGrid(room, authentication.getName(), request.getGridId());
        roomBroadcastService.broadcastState(code, result);
        return result;
    }

    @PostMapping("/skip")
    public GridBattleStateDto skip(@PathVariable String code, Authentication authentication) {
        GameRoom room = roomService.findByCode(code);
        GridBattleStateDto result = gridBattleOnlineService.skip(room, authentication.getName());
        roomBroadcastService.broadcastState(code, result);
        return result;
    }

    @PostMapping("/next-grid")
    public GridBattleStateDto nextGrid(@PathVariable String code, Authentication authentication) {
        GameRoom room = roomService.findByCode(code);
        GridBattleStateDto result = gridBattleOnlineService.advanceToNextGrid(room, authentication.getName());
        roomBroadcastService.broadcastState(code, result);
        return result;
    }
}
