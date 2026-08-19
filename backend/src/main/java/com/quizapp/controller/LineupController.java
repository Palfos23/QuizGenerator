package com.quizapp.controller;

import com.quizapp.dto.AthleteDto;
import com.quizapp.dto.GuessRequest;
import com.quizapp.dto.LineupGuessRequest;
import com.quizapp.dto.LineupGuessResultDto;
import com.quizapp.dto.LineupPlayStateDto;
import com.quizapp.dto.LineupScoreboardDto;
import com.quizapp.dto.LineupSummaryDto;
import com.quizapp.service.LineupPlayService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/lineups")
public class LineupController {

    private final LineupPlayService lineupPlayService;

    public LineupController(LineupPlayService lineupPlayService) {
        this.lineupPlayService = lineupPlayService;
    }

    @GetMapping
    public List<LineupSummaryDto> findAll(Authentication authentication) {
        return lineupPlayService.findAll(authentication.getName());
    }

    /** Random Starting XI Battle's round-start "choose one of 3" picker - see LineupPlayService.getBattleRoundChoices. */
    @GetMapping("/battle-round-choices")
    public List<LineupSummaryDto> battleRoundChoices(
            @RequestParam(defaultValue = "3") int count,
            @RequestParam(required = false) List<Long> excludeIds) {
        return lineupPlayService.getBattleRoundChoices(count, excludeIds);
    }

    @GetMapping("/{id}/scoreboard")
    public LineupScoreboardDto scoreboard(@PathVariable Long id, Authentication authentication) {
        return lineupPlayService.getScoreboard(id, authentication.getName());
    }

    @PutMapping("/{id}/leaderboard-preference")
    public void setLeaderboardPreference(@PathVariable Long id, @RequestParam boolean include, Authentication authentication) {
        lineupPlayService.setLeaderboardPreference(id, authentication.getName(), include);
    }

    @GetMapping("/{id}/play")
    public LineupPlayStateDto play(@PathVariable Long id, Authentication authentication) {
        return lineupPlayService.getPlayState(id, authentication.getName());
    }

    @GetMapping("/{id}/candidates")
    public List<AthleteDto> candidates(@PathVariable Long id, @RequestParam(required = false) String search) {
        return lineupPlayService.searchCandidates(id, search);
    }

    /** Starting shirts for a local pass-and-play multiplayer game - no persisted attempt involved. */
    @GetMapping("/{id}/multiplayer-start")
    public LineupPlayStateDto multiplayerStart(@PathVariable Long id) {
        return lineupPlayService.getMultiplayerStartState(id);
    }

    /** Every slot's answer, for showing what wasn't found once a pass-and-play round has ended. */
    @GetMapping("/{id}/multiplayer-reveal")
    public Map<Long, String> multiplayerReveal(@PathVariable Long id) {
        return lineupPlayService.getMultiplayerReveal(id);
    }

    /** Stateless guess check for multiplayer mode - the client tracks revealed slots itself. */
    @PostMapping("/{id}/multiplayer-guess")
    public LineupGuessResultDto multiplayerGuess(@PathVariable Long id, @Valid @RequestBody LineupGuessRequest request) {
        return lineupPlayService.multiplayerGuess(id, request);
    }

    @PostMapping("/{id}/guess")
    public LineupGuessResultDto guess(@PathVariable Long id, @Valid @RequestBody GuessRequest request, Authentication authentication) {
        return lineupPlayService.guess(id, authentication.getName(), request.getAthleteId());
    }

    @PostMapping("/{id}/reveal")
    public LineupPlayStateDto reveal(@PathVariable Long id, Authentication authentication) {
        return lineupPlayService.reveal(id, authentication.getName());
    }
}
