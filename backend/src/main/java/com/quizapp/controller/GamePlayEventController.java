package com.quizapp.controller;

import com.quizapp.model.BattleGameType;
import com.quizapp.service.GamePlayEventService;
import com.quizapp.service.PlayAccessService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// Online battle games record their own play-count event server-side, right
// where each *OnlineService transitions a room to finished (exactly once,
// since that transition itself only ever happens once per room - see
// GamePlayEvent). Pass-and-play games have no server-side state to hook
// into at all - the whole session lives in the browser - so this is the one
// place a finished pass-and-play game gets recorded, self-reported by the
// client at the exact moment its own "game over" screen appears.
@RestController
@RequestMapping("/api/stats")
public class GamePlayEventController {

    private final GamePlayEventService gamePlayEventService;
    private final PlayAccessService playAccessService;

    public GamePlayEventController(GamePlayEventService gamePlayEventService, PlayAccessService playAccessService) {
        this.gamePlayEventService = gamePlayEventService;
        this.playAccessService = playAccessService;
    }

    @PostMapping("/game-played")
    public void gamePlayed(@RequestParam BattleGameType gameType, Authentication authentication) {
        playAccessService.requireAccessForBattleGameType(authentication, gameType);
        gamePlayEventService.record(gameType);
    }
}
