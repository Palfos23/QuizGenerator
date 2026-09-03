package com.quizapp.controller;

import com.quizapp.dto.AthleteDto;
import com.quizapp.dto.PenaltyGuessRequest;
import com.quizapp.dto.PenaltyGuessResultDto;
import com.quizapp.dto.PenaltyKickDto;
import com.quizapp.dto.PenaltyShootoutPlayStateDto;
import com.quizapp.dto.PenaltyShootoutSummaryDto;
import com.quizapp.service.PenaltyShootoutPlayService;
import com.quizapp.service.PlayAccessService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/penalty-shootouts")
public class PenaltyShootoutController {

    private final PenaltyShootoutPlayService penaltyShootoutPlayService;
    private final PlayAccessService playAccessService;

    public PenaltyShootoutController(PenaltyShootoutPlayService penaltyShootoutPlayService,
                                      PlayAccessService playAccessService) {
        this.penaltyShootoutPlayService = penaltyShootoutPlayService;
        this.playAccessService = playAccessService;
    }

    /** The full pool, for "pick my own" board selection. */
    @GetMapping
    public List<PenaltyShootoutSummaryDto> findAll(Authentication authentication) {
        playAccessService.requirePenaltyShootoutAccess(authentication);
        return penaltyShootoutPlayService.findAll();
    }

    /** "Random" round-start picker - see PenaltyShootoutPlayService.getRoundChoices. */
    @GetMapping("/round-choices")
    public List<PenaltyShootoutSummaryDto> roundChoices(
            @RequestParam(defaultValue = "3") int count,
            @RequestParam(required = false) List<Long> excludeIds,
            Authentication authentication) {
        playAccessService.requirePenaltyShootoutAccess(authentication);
        return penaltyShootoutPlayService.getRoundChoices(count, excludeIds);
    }

    /** Starting state for a solo or local pass-and-play game - no persisted attempt involved. */
    @GetMapping("/{id}/start")
    public PenaltyShootoutPlayStateDto start(@PathVariable Long id, Authentication authentication) {
        playAccessService.requirePenaltyShootoutAccess(authentication);
        return penaltyShootoutPlayService.getStartState(id);
    }

    @GetMapping("/{id}/candidates")
    public List<AthleteDto> candidates(@PathVariable Long id, @RequestParam(required = false) String search) {
        return penaltyShootoutPlayService.searchCandidates(id, search);
    }

    /** Stateless guess check - the client tracks revealed kicks itself. */
    @PostMapping("/{id}/guess")
    public PenaltyGuessResultDto guess(@PathVariable Long id, @Valid @RequestBody PenaltyGuessRequest request) {
        return penaltyShootoutPlayService.guess(id, request);
    }

    /** Every kick's answer (name only), for showing what wasn't found once a round has ended. */
    @GetMapping("/{id}/reveal")
    public Map<Long, String> reveal(@PathVariable Long id) {
        return penaltyShootoutPlayService.getReveal(id);
    }

    /** Every kick fully revealed (name + photo), for a results-screen recap. */
    @GetMapping("/{id}/reveal-all")
    public List<PenaltyKickDto> revealAll(@PathVariable Long id) {
        return penaltyShootoutPlayService.revealAll(id);
    }
}
