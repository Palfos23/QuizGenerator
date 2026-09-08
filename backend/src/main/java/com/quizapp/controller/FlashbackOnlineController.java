package com.quizapp.controller;

import com.quizapp.dto.FlashbackOnlineGuessRequest;
import com.quizapp.dto.FlashbackOnlineStateDto;
import com.quizapp.model.GameRoom;
import com.quizapp.service.FlashbackOnlineService;
import com.quizapp.service.RoomBroadcastService;
import com.quizapp.service.RoomService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rooms/{code}/flashback")
public class FlashbackOnlineController {

    private final RoomService roomService;
    private final FlashbackOnlineService flashbackOnlineService;
    private final RoomBroadcastService roomBroadcastService;

    public FlashbackOnlineController(RoomService roomService, FlashbackOnlineService flashbackOnlineService,
                                      RoomBroadcastService roomBroadcastService) {
        this.roomService = roomService;
        this.flashbackOnlineService = flashbackOnlineService;
        this.roomBroadcastService = roomBroadcastService;
    }

    /** Still exposed for the initial fetch on mount, and as this room's poll-based
     *  fallback if the WebSocket push below can't connect - see useRoomChannel.js. */
    @GetMapping("/state")
    public FlashbackOnlineStateDto state(@PathVariable String code, Authentication authentication) {
        GameRoom room = roomService.findByCode(code);
        return flashbackOnlineService.getState(room, authentication.getName());
    }

    @PostMapping("/guess")
    public FlashbackOnlineStateDto guess(@PathVariable String code, @Valid @RequestBody FlashbackOnlineGuessRequest request,
                                          Authentication authentication) {
        GameRoom room = roomService.findByCode(code);
        FlashbackOnlineStateDto result = flashbackOnlineService.submitGuess(room, authentication.getName(), request.getYear());
        roomBroadcastService.broadcastState(code, result);
        return result;
    }

    @PostMapping("/next-round")
    public FlashbackOnlineStateDto nextRound(@PathVariable String code, Authentication authentication) {
        GameRoom room = roomService.findByCode(code);
        FlashbackOnlineStateDto result = flashbackOnlineService.nextRound(room, authentication.getName());
        roomBroadcastService.broadcastState(code, result);
        return result;
    }
}
