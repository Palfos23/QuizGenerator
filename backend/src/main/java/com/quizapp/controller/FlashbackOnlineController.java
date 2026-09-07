package com.quizapp.controller;

import com.quizapp.dto.FlashbackOnlineGuessRequest;
import com.quizapp.dto.FlashbackOnlineStateDto;
import com.quizapp.model.GameRoom;
import com.quizapp.service.FlashbackOnlineService;
import com.quizapp.service.RoomService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rooms/{code}/flashback")
public class FlashbackOnlineController {

    private final RoomService roomService;
    private final FlashbackOnlineService flashbackOnlineService;

    public FlashbackOnlineController(RoomService roomService, FlashbackOnlineService flashbackOnlineService) {
        this.roomService = roomService;
        this.flashbackOnlineService = flashbackOnlineService;
    }

    /** Polled repeatedly by every participant's client. */
    @GetMapping("/state")
    public FlashbackOnlineStateDto state(@PathVariable String code, Authentication authentication) {
        GameRoom room = roomService.findByCode(code);
        return flashbackOnlineService.getState(room, authentication.getName());
    }

    @PostMapping("/guess")
    public FlashbackOnlineStateDto guess(@PathVariable String code, @Valid @RequestBody FlashbackOnlineGuessRequest request,
                                          Authentication authentication) {
        GameRoom room = roomService.findByCode(code);
        return flashbackOnlineService.submitGuess(room, authentication.getName(), request.getYear());
    }

    @PostMapping("/next-round")
    public FlashbackOnlineStateDto nextRound(@PathVariable String code, Authentication authentication) {
        GameRoom room = roomService.findByCode(code);
        return flashbackOnlineService.nextRound(room, authentication.getName());
    }
}
