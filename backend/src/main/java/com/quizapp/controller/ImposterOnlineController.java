package com.quizapp.controller;

import com.quizapp.dto.ImposterChooseRequest;
import com.quizapp.dto.ImposterOnlineFlipRequest;
import com.quizapp.dto.ImposterOnlineRevealDto;
import com.quizapp.dto.ImposterOnlineStateDto;
import com.quizapp.model.GameRoom;
import com.quizapp.service.ImposterOnlineService;
import com.quizapp.service.RoomBroadcastService;
import com.quizapp.service.RoomService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rooms/{code}/imposter")
public class ImposterOnlineController {

    private final RoomService roomService;
    private final ImposterOnlineService imposterOnlineService;
    private final RoomBroadcastService roomBroadcastService;

    public ImposterOnlineController(RoomService roomService, ImposterOnlineService imposterOnlineService,
                                     RoomBroadcastService roomBroadcastService) {
        this.roomService = roomService;
        this.imposterOnlineService = imposterOnlineService;
        this.roomBroadcastService = roomBroadcastService;
    }

    /** Still exposed for the initial fetch on mount, and as this room's poll-based
     *  fallback if the WebSocket push below can't connect - see useRoomChannel.js. */
    @GetMapping("/state")
    public ImposterOnlineStateDto state(@PathVariable String code, Authentication authentication) {
        GameRoom room = roomService.findByCode(code);
        return imposterOnlineService.getState(room, authentication.getName());
    }

    @PostMapping("/choose-grid")
    public ImposterOnlineStateDto chooseGrid(@PathVariable String code, @Valid @RequestBody ImposterChooseRequest request,
                                              Authentication authentication) {
        GameRoom room = roomService.findByCode(code);
        ImposterOnlineStateDto result = imposterOnlineService.chooseGrid(room, authentication.getName(), request.getGridId());
        roomBroadcastService.broadcastState(code, result);
        return result;
    }

    @PostMapping("/flip")
    public ImposterOnlineStateDto flip(@PathVariable String code, @Valid @RequestBody ImposterOnlineFlipRequest request,
                                        Authentication authentication) {
        GameRoom room = roomService.findByCode(code);
        ImposterOnlineStateDto result = imposterOnlineService.flip(room, authentication.getName(), request.getTileId());
        roomBroadcastService.broadcastState(code, result);
        return result;
    }

    // Not broadcast - a one-time "show me the answers" fetch after the board's
    // already finished, not a change in room state anyone else needs to hear about.
    @GetMapping("/reveal")
    public List<ImposterOnlineRevealDto> reveal(@PathVariable String code, Authentication authentication) {
        GameRoom room = roomService.findByCode(code);
        return imposterOnlineService.getReveal(room, authentication.getName());
    }

    @PostMapping("/next-board")
    public ImposterOnlineStateDto nextBoard(@PathVariable String code, Authentication authentication) {
        GameRoom room = roomService.findByCode(code);
        ImposterOnlineStateDto result = imposterOnlineService.advanceToNextBoard(room, authentication.getName());
        roomBroadcastService.broadcastState(code, result);
        return result;
    }
}
