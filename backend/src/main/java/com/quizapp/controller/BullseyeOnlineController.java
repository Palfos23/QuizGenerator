package com.quizapp.controller;

import com.quizapp.dto.BullseyeOnlineAnswerRequest;
import com.quizapp.dto.BullseyeOnlineStateDto;
import com.quizapp.model.GameRoom;
import com.quizapp.service.BullseyeOnlineService;
import com.quizapp.service.RoomService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rooms/{code}/bullseye")
public class BullseyeOnlineController {

    private final RoomService roomService;
    private final BullseyeOnlineService bullseyeOnlineService;

    public BullseyeOnlineController(RoomService roomService, BullseyeOnlineService bullseyeOnlineService) {
        this.roomService = roomService;
        this.bullseyeOnlineService = bullseyeOnlineService;
    }

    /** Polled repeatedly by every participant's client. */
    @GetMapping("/state")
    public BullseyeOnlineStateDto state(@PathVariable String code, Authentication authentication) {
        GameRoom room = roomService.findByCode(code);
        return bullseyeOnlineService.getState(room, authentication.getName());
    }

    @PostMapping("/answer")
    public BullseyeOnlineStateDto answer(@PathVariable String code, @Valid @RequestBody BullseyeOnlineAnswerRequest request,
                                          Authentication authentication) {
        GameRoom room = roomService.findByCode(code);
        return bullseyeOnlineService.submitAnswer(room, authentication.getName(), request.getGuessedName());
    }

    @PostMapping("/next-round")
    public BullseyeOnlineStateDto nextRound(@PathVariable String code, Authentication authentication) {
        GameRoom room = roomService.findByCode(code);
        return bullseyeOnlineService.nextRound(room, authentication.getName());
    }
}
