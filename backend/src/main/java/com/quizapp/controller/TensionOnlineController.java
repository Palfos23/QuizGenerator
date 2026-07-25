package com.quizapp.controller;

import com.quizapp.dto.TensionOnlineAnswerRequest;
import com.quizapp.dto.TensionOnlineStateDto;
import com.quizapp.model.GameRoom;
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

    public TensionOnlineController(RoomService roomService, TensionOnlineService tensionOnlineService) {
        this.roomService = roomService;
        this.tensionOnlineService = tensionOnlineService;
    }

    /** Polled repeatedly by every participant's client. */
    @GetMapping("/state")
    public TensionOnlineStateDto state(@PathVariable String code, Authentication authentication) {
        GameRoom room = roomService.findByCode(code);
        return tensionOnlineService.getState(room, authentication.getName());
    }

    @PostMapping("/answer")
    public TensionOnlineStateDto answer(@PathVariable String code, @Valid @RequestBody TensionOnlineAnswerRequest request,
                                         Authentication authentication) {
        GameRoom room = roomService.findByCode(code);
        return tensionOnlineService.submitAnswer(room, authentication.getName(), request.getAnswerText());
    }

    @PostMapping("/next-question")
    public TensionOnlineStateDto nextQuestion(@PathVariable String code, Authentication authentication) {
        GameRoom room = roomService.findByCode(code);
        return tensionOnlineService.nextQuestion(room, authentication.getName());
    }
}
