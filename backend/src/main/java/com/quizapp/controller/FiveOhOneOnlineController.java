package com.quizapp.controller;

import com.quizapp.dto.FiveOhOneOnlineStateDto;
import com.quizapp.dto.FiveOhOneThrowRequest;
import com.quizapp.model.GameRoom;
import com.quizapp.service.FiveOhOneOnlineService;
import com.quizapp.service.RoomBroadcastService;
import com.quizapp.service.RoomService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rooms/{code}/501")
public class FiveOhOneOnlineController {

    private final RoomService roomService;
    private final FiveOhOneOnlineService fiveOhOneOnlineService;
    private final RoomBroadcastService roomBroadcastService;

    public FiveOhOneOnlineController(RoomService roomService, FiveOhOneOnlineService fiveOhOneOnlineService,
                                      RoomBroadcastService roomBroadcastService) {
        this.roomService = roomService;
        this.fiveOhOneOnlineService = fiveOhOneOnlineService;
        this.roomBroadcastService = roomBroadcastService;
    }

    /** Still exposed for the initial fetch on mount, and as this room's poll-based
     *  fallback if the WebSocket push below can't connect - see useRoomChannel.js. */
    @GetMapping("/state")
    public FiveOhOneOnlineStateDto state(@PathVariable String code, Authentication authentication) {
        GameRoom room = roomService.findByCode(code);
        return fiveOhOneOnlineService.getState(room, authentication.getName());
    }

    @PostMapping("/throw")
    public FiveOhOneOnlineStateDto throwEntry(@PathVariable String code, @Valid @RequestBody FiveOhOneThrowRequest request,
                                               Authentication authentication) {
        GameRoom room = roomService.findByCode(code);
        FiveOhOneOnlineStateDto result = fiveOhOneOnlineService.throwEntry(room, authentication.getName(), request.getEntryId());
        roomBroadcastService.broadcastState(code, result);
        return result;
    }
}
