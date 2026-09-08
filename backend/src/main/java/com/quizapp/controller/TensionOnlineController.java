package com.quizapp.controller;

import com.quizapp.dto.TensionOnlineAnswerRequest;
import com.quizapp.dto.TensionOnlineStateDto;
import com.quizapp.model.GameRoom;
import com.quizapp.service.RoomBroadcastService;
import com.quizapp.service.RoomService;
import com.quizapp.service.TensionOnlineService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rooms/{code}/tension")
public class TensionOnlineController {

    private final RoomService roomService;
    private final TensionOnlineService tensionOnlineService;
    private final RoomBroadcastService roomBroadcastService;

    public TensionOnlineController(RoomService roomService, TensionOnlineService tensionOnlineService,
                                    RoomBroadcastService roomBroadcastService) {
        this.roomService = roomService;
        this.tensionOnlineService = tensionOnlineService;
        this.roomBroadcastService = roomBroadcastService;
    }

    /** Still exposed for the initial fetch on mount, and as this room's poll-based
     *  fallback if the WebSocket push below can't connect - see useRoomChannel.js. */
    @GetMapping("/state")
    public TensionOnlineStateDto state(@PathVariable String code, Authentication authentication) {
        GameRoom room = roomService.findByCode(code);
        return tensionOnlineService.getState(room, authentication.getName());
    }

    @PostMapping("/answer")
    public TensionOnlineStateDto answer(@PathVariable String code, @Valid @RequestBody TensionOnlineAnswerRequest request,
                                         Authentication authentication) {
        GameRoom room = roomService.findByCode(code);
        TensionOnlineStateDto result = tensionOnlineService.submitAnswer(room, authentication.getName(), request.getAnswerText());
        roomBroadcastService.broadcastState(code, result);
        return result;
    }

    @PostMapping("/next-question")
    public TensionOnlineStateDto nextQuestion(@PathVariable String code, Authentication authentication) {
        GameRoom room = roomService.findByCode(code);
        TensionOnlineStateDto result = tensionOnlineService.nextQuestion(room, authentication.getName());
        roomBroadcastService.broadcastState(code, result);
        return result;
    }
}
