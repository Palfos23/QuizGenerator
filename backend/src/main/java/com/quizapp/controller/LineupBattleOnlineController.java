package com.quizapp.controller;

import com.quizapp.dto.LineupBattleGuessRequest;
import com.quizapp.dto.LineupBattleStateDto;
import com.quizapp.model.GameRoom;
import com.quizapp.service.LineupBattleOnlineService;
import com.quizapp.service.RoomService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rooms/{code}/lineup-battle")
public class LineupBattleOnlineController {

    private final RoomService roomService;
    private final LineupBattleOnlineService lineupBattleOnlineService;

    public LineupBattleOnlineController(RoomService roomService, LineupBattleOnlineService lineupBattleOnlineService) {
        this.roomService = roomService;
        this.lineupBattleOnlineService = lineupBattleOnlineService;
    }

    /** Polled repeatedly by every participant's client - this is how state stays in sync. */
    @GetMapping("/state")
    public LineupBattleStateDto state(@PathVariable String code, Authentication authentication) {
        GameRoom room = roomService.findByCode(code);
        return lineupBattleOnlineService.getState(room, authentication.getName());
    }

    @PostMapping("/guess")
    public LineupBattleStateDto guess(@PathVariable String code, @Valid @RequestBody LineupBattleGuessRequest request,
                                       Authentication authentication) {
        GameRoom room = roomService.findByCode(code);
        return lineupBattleOnlineService.guess(room, authentication.getName(), request.getAthleteId());
    }

    @PostMapping("/skip")
    public LineupBattleStateDto skip(@PathVariable String code, Authentication authentication) {
        GameRoom room = roomService.findByCode(code);
        return lineupBattleOnlineService.skip(room, authentication.getName());
    }

    @PostMapping("/next-lineup")
    public LineupBattleStateDto nextLineup(@PathVariable String code, Authentication authentication) {
        GameRoom room = roomService.findByCode(code);
        return lineupBattleOnlineService.advanceToNextLineup(room, authentication.getName());
    }
}
