package com.quizapp.controller;

import com.quizapp.dto.AthleteDto;
import com.quizapp.dto.GridPlayStateDto;
import com.quizapp.dto.GridSummaryDto;
import com.quizapp.dto.GuessRequest;
import com.quizapp.dto.GuessResultDto;
import com.quizapp.service.GridPlayService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/grids")
public class GridController {

    private final GridPlayService gridPlayService;

    public GridController(GridPlayService gridPlayService) {
        this.gridPlayService = gridPlayService;
    }

    @GetMapping("/active")
    public List<GridSummaryDto> active(Authentication authentication) {
        return gridPlayService.findActive(authentication.getName());
    }

    @GetMapping("/{id}/scoreboard")
    public com.quizapp.dto.GridScoreboardDto scoreboard(@PathVariable Long id, Authentication authentication) {
        return gridPlayService.getScoreboard(id, authentication.getName());
    }

    @GetMapping("/{id}/reveal-all")
    public List<com.quizapp.dto.GridEntryViewDto> revealAll(@PathVariable Long id) {
        return gridPlayService.revealAllEntries(id);
    }

    @GetMapping("/archive")
    public List<GridSummaryDto> archive(Authentication authentication) {
        return gridPlayService.findArchive(authentication.getName());
    }

    @GetMapping("/future")
    public List<GridSummaryDto> future(Authentication authentication) {
        return gridPlayService.findFuture(authentication.getName());
    }

    @GetMapping("/battle-eligible")
    public List<GridSummaryDto> battleEligible(Authentication authentication) {
        return gridPlayService.findEligibleForGridBattle(authentication.getName());
    }

    @GetMapping("/{id}/play")
    public GridPlayStateDto play(@PathVariable Long id, Authentication authentication) {
        return gridPlayService.getPlayState(id, authentication.getName());
    }

    @GetMapping("/{id}/candidates")
    public List<AthleteDto> candidates(@PathVariable Long id, @RequestParam(required = false) String search) {
        return gridPlayService.searchCandidates(id, search);
    }

    /** Starting tiles for a local pass-and-play multiplayer game - no persisted attempt involved. */
    @GetMapping("/{id}/multiplayer-start")
    public GridPlayStateDto multiplayerStart(@PathVariable Long id) {
        return gridPlayService.getMultiplayerStartState(id);
    }

    /** Stateless guess check for multiplayer mode - the client tracks revealed entries itself. */
    @PostMapping("/{id}/multiplayer-guess")
    public GuessResultDto multiplayerGuess(@PathVariable Long id, @jakarta.validation.Valid @RequestBody com.quizapp.dto.MultiplayerGuessRequest request) {
        return gridPlayService.multiplayerGuess(id, request);
    }

    @PostMapping("/{id}/guess")
    public GuessResultDto guess(@PathVariable Long id, @Valid @RequestBody GuessRequest request, Authentication authentication) {
        return gridPlayService.guess(id, authentication.getName(), request.getAthleteId());
    }

    @PostMapping("/{id}/overtime")
    public GridPlayStateDto overtime(@PathVariable Long id, Authentication authentication) {
        return gridPlayService.enterOvertime(id, authentication.getName());
    }

    @PostMapping("/{id}/reveal")
    public GridPlayStateDto reveal(@PathVariable Long id, Authentication authentication) {
        return gridPlayService.reveal(id, authentication.getName());
    }
}
