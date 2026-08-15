package com.quizapp.controller;

import com.quizapp.dto.ImposterOnlineFlipRequest;
import com.quizapp.dto.ImposterOnlineRevealDto;
import com.quizapp.dto.ImposterOnlineStateDto;
import com.quizapp.model.GameRoom;
import com.quizapp.service.ImposterOnlineService;
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

    public ImposterOnlineController(RoomService roomService, ImposterOnlineService imposterOnlineService) {
        this.roomService = roomService;
        this.imposterOnlineService = imposterOnlineService;
    }

    /** Polled repeatedly by every participant's client - this is how state stays in sync. */
    @GetMapping("/state")
    public ImposterOnlineStateDto state(@PathVariable String code, Authentication authentication) {
        GameRoom room = roomService.findByCode(code);
        return imposterOnlineService.getState(room, authentication.getName());
    }

    @PostMapping("/flip")
    public ImposterOnlineStateDto flip(@PathVariable String code, @Valid @RequestBody ImposterOnlineFlipRequest request,
                                        Authentication authentication) {
        GameRoom room = roomService.findByCode(code);
        return imposterOnlineService.flip(room, authentication.getName(), request.getTileId());
    }

    @GetMapping("/reveal")
    public List<ImposterOnlineRevealDto> reveal(@PathVariable String code, Authentication authentication) {
        GameRoom room = roomService.findByCode(code);
        return imposterOnlineService.getReveal(room, authentication.getName());
    }

    @PostMapping("/next-board")
    public ImposterOnlineStateDto nextBoard(@PathVariable String code, Authentication authentication) {
        GameRoom room = roomService.findByCode(code);
        return imposterOnlineService.advanceToNextBoard(room, authentication.getName());
    }
}
