package com.quizapp.controller;

import com.quizapp.dto.AthleteDto;
import com.quizapp.dto.GridCategoryDto;
import com.quizapp.dto.GridPlayStateDto;
import com.quizapp.dto.GridSummaryDto;
import com.quizapp.dto.GuessRequest;
import com.quizapp.dto.GuessResultDto;
import com.quizapp.service.GridCategoryService;
import com.quizapp.service.GridPlayService;
import com.quizapp.service.PlayAccessService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/grids")
public class GridController {

    private final GridPlayService gridPlayService;
    private final GridCategoryService gridCategoryService;
    private final PlayAccessService playAccessService;

    public GridController(GridPlayService gridPlayService, GridCategoryService gridCategoryService,
                           PlayAccessService playAccessService) {
        this.gridPlayService = gridPlayService;
        this.gridCategoryService = gridCategoryService;
        this.playAccessService = playAccessService;
    }

    @GetMapping("/categories")
    public List<GridCategoryDto> categories() {
        return gridCategoryService.findAll();
    }

    @GetMapping("/active")
    public List<GridSummaryDto> active(Authentication authentication) {
        return gridPlayService.findActive(authentication.getName());
    }

    @GetMapping("/{id}/scoreboard")
    public com.quizapp.dto.GridScoreboardDto scoreboard(@PathVariable Long id, Authentication authentication) {
        return gridPlayService.getScoreboard(id, authentication.getName());
    }

    @PutMapping("/{id}/leaderboard-preference")
    public void setLeaderboardPreference(@PathVariable Long id, @RequestParam boolean include, Authentication authentication) {
        gridPlayService.setLeaderboardPreference(id, authentication.getName(), include);
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
        playAccessService.requireGridBattleAccess(authentication);
        return gridPlayService.findEligibleForGridBattle(authentication.getName());
    }

    /** Random Grid Battle's round-start "choose one of 3" picker - see GridPlayService.getBattleRoundChoices. */
    @GetMapping("/battle-round-choices")
    public List<GridSummaryDto> battleRoundChoices(
            @RequestParam(defaultValue = "3") int count,
            @RequestParam(required = false) List<Long> excludeIds,
            Authentication authentication) {
        playAccessService.requireGridBattleAccess(authentication);
        return gridPlayService.getBattleRoundChoices(count, excludeIds);
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
    public GridPlayStateDto multiplayerStart(@PathVariable Long id, Authentication authentication) {
        playAccessService.requireGridBattleAccess(authentication);
        return gridPlayService.getMultiplayerStartState(id);
    }

    /** Every entry's answer, for showing what wasn't found once a grid battle round has ended. */
    @GetMapping("/{id}/multiplayer-reveal")
    public java.util.Map<Long, String> multiplayerReveal(@PathVariable Long id) {
        return gridPlayService.getMultiplayerReveal(id);
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
