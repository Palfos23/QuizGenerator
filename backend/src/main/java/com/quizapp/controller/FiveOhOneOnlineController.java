package com.quizapp.controller;

import com.quizapp.dto.FiveOhOneOnlineStateDto;
import com.quizapp.dto.FiveOhOneThrowRequest;
import com.quizapp.model.GameRoom;
import com.quizapp.service.FiveOhOneOnlineService;
import com.quizapp.service.RoomService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rooms/{code}/501")
public class FiveOhOneOnlineController {

    private final RoomService roomService;
    private final FiveOhOneOnlineService fiveOhOneOnlineService;

    public FiveOhOneOnlineController(RoomService roomService, FiveOhOneOnlineService fiveOhOneOnlineService) {
        this.roomService = roomService;
        this.fiveOhOneOnlineService = fiveOhOneOnlineService;
    }

    /** Polled repeatedly by every participant's client - this is how state stays in sync. */
    @GetMapping("/state")
    public FiveOhOneOnlineStateDto state(@PathVariable String code, Authentication authentication) {
        GameRoom room = roomService.findByCode(code);
        return fiveOhOneOnlineService.getState(room, authentication.getName());
    }

    @PostMapping("/throw")
    public FiveOhOneOnlineStateDto throwEntry(@PathVariable String code, @Valid @RequestBody FiveOhOneThrowRequest request,
                                               Authentication authentication) {
        GameRoom room = roomService.findByCode(code);
        return fiveOhOneOnlineService.throwEntry(room, authentication.getName(), request.getEntryId());
    }
}
