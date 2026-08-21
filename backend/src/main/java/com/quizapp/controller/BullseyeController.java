package com.quizapp.controller;

import com.quizapp.dto.BullseyeQuestionSummaryDto;
import com.quizapp.dto.BullseyeRoundStateDto;
import com.quizapp.service.BullseyePlayService;
import com.quizapp.service.PlayAccessService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bullseye")
public class BullseyeController {

    private final BullseyePlayService bullseyePlayService;
    private final PlayAccessService playAccessService;

    public BullseyeController(BullseyePlayService bullseyePlayService, PlayAccessService playAccessService) {
        this.bullseyePlayService = bullseyePlayService;
        this.playAccessService = playAccessService;
    }

    @GetMapping("/battle-eligible")
    public List<BullseyeQuestionSummaryDto> battleEligible(Authentication authentication) {
        playAccessService.requireBullseyeAccess(authentication);
        return bullseyePlayService.findEligible();
    }

    /** Random round-start "choose one of 3" picker - see BullseyePlayService.getBattleRoundChoices. */
    @GetMapping("/battle-round-choices")
    public List<BullseyeQuestionSummaryDto> battleRoundChoices(
            @RequestParam(defaultValue = "3") int count,
            @RequestParam(required = false) List<Long> excludeIds,
            Authentication authentication) {
        playAccessService.requireBullseyeAccess(authentication);
        return bullseyePlayService.getBattleRoundChoices(count, excludeIds);
    }

    /** Starting state for a chosen round - includes the full answer key, no persisted attempt involved. */
    @GetMapping("/{id}/multiplayer-start")
    public BullseyeRoundStateDto multiplayerStart(@PathVariable Long id, Authentication authentication) {
        playAccessService.requireBullseyeAccess(authentication);
        return bullseyePlayService.getMultiplayerStartState(id);
    }
}
