package com.quizapp.controller;

import com.quizapp.dto.BullseyeOnlineAnswerRequest;
import com.quizapp.dto.BullseyeOnlineStateDto;
import com.quizapp.model.GameRoom;
import com.quizapp.service.BullseyeOnlineService;
import com.quizapp.service.RoomBroadcastService;
import com.quizapp.service.RoomService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rooms/{code}/bullseye")
public class BullseyeOnlineController {

    private final RoomService roomService;
    private final BullseyeOnlineService bullseyeOnlineService;
    private final RoomBroadcastService roomBroadcastService;

    public BullseyeOnlineController(RoomService roomService, BullseyeOnlineService bullseyeOnlineService,
                                     RoomBroadcastService roomBroadcastService) {
        this.roomService = roomService;
        this.bullseyeOnlineService = bullseyeOnlineService;
        this.roomBroadcastService = roomBroadcastService;
    }

    /** Still exposed for the initial fetch on mount, and as this room's poll-based
     *  fallback if the WebSocket push below can't connect - see useRoomChannel.js. */
    @GetMapping("/state")
    public BullseyeOnlineStateDto state(@PathVariable String code, Authentication authentication) {
        GameRoom room = roomService.findByCode(code);
        return bullseyeOnlineService.getState(room, authentication.getName());
    }

    @PostMapping("/answer")
    public BullseyeOnlineStateDto answer(@PathVariable String code, @Valid @RequestBody BullseyeOnlineAnswerRequest request,
                                          Authentication authentication) {
        GameRoom room = roomService.findByCode(code);
        BullseyeOnlineStateDto result = bullseyeOnlineService.submitAnswer(room, authentication.getName(), request.getGuessedName());
        roomBroadcastService.broadcastState(code, result);
        return result;
    }

    @PostMapping("/next-round")
    public BullseyeOnlineStateDto nextRound(@PathVariable String code, Authentication authentication) {
        GameRoom room = roomService.findByCode(code);
        BullseyeOnlineStateDto result = bullseyeOnlineService.nextRound(room, authentication.getName());
        roomBroadcastService.broadcastState(code, result);
        return result;
    }
}
