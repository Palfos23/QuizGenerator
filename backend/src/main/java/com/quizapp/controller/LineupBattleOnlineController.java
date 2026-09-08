package com.quizapp.controller;

import com.quizapp.dto.LineupBattleChooseRequest;
import com.quizapp.dto.LineupBattleGuessRequest;
import com.quizapp.dto.LineupBattleStateDto;
import com.quizapp.model.GameRoom;
import com.quizapp.service.LineupBattleOnlineService;
import com.quizapp.service.RoomBroadcastService;
import com.quizapp.service.RoomService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rooms/{code}/lineup-battle")
public class LineupBattleOnlineController {

    private final RoomService roomService;
    private final LineupBattleOnlineService lineupBattleOnlineService;
    private final RoomBroadcastService roomBroadcastService;

    public LineupBattleOnlineController(RoomService roomService, LineupBattleOnlineService lineupBattleOnlineService,
                                         RoomBroadcastService roomBroadcastService) {
        this.roomService = roomService;
        this.lineupBattleOnlineService = lineupBattleOnlineService;
        this.roomBroadcastService = roomBroadcastService;
    }

    /** Still exposed for the initial fetch on mount, and as this room's poll-based
     *  fallback if the WebSocket push below can't connect - see useRoomChannel.js. */
    @GetMapping("/state")
    public LineupBattleStateDto state(@PathVariable String code, Authentication authentication) {
        GameRoom room = roomService.findByCode(code);
        return lineupBattleOnlineService.getState(room, authentication.getName());
    }

    @PostMapping("/guess")
    public LineupBattleStateDto guess(@PathVariable String code, @Valid @RequestBody LineupBattleGuessRequest request,
                                       Authentication authentication) {
        GameRoom room = roomService.findByCode(code);
        LineupBattleStateDto result = lineupBattleOnlineService.guess(room, authentication.getName(), request.getAthleteId());
        roomBroadcastService.broadcastState(code, result);
        return result;
    }

    @PostMapping("/choose-lineup")
    public LineupBattleStateDto chooseLineup(@PathVariable String code, @Valid @RequestBody LineupBattleChooseRequest request,
                                              Authentication authentication) {
        GameRoom room = roomService.findByCode(code);
        LineupBattleStateDto result = lineupBattleOnlineService.chooseLineup(room, authentication.getName(), request.getLineupId());
        roomBroadcastService.broadcastState(code, result);
        return result;
    }

    @PostMapping("/skip")
    public LineupBattleStateDto skip(@PathVariable String code, Authentication authentication) {
        GameRoom room = roomService.findByCode(code);
        LineupBattleStateDto result = lineupBattleOnlineService.skip(room, authentication.getName());
        roomBroadcastService.broadcastState(code, result);
        return result;
    }

    @PostMapping("/next-lineup")
    public LineupBattleStateDto nextLineup(@PathVariable String code, Authentication authentication) {
        GameRoom room = roomService.findByCode(code);
        LineupBattleStateDto result = lineupBattleOnlineService.advanceToNextLineup(room, authentication.getName());
        roomBroadcastService.broadcastState(code, result);
        return result;
    }
}
