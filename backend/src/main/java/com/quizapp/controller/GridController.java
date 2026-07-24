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
    public List<com.quizapp.dto.GridScoreboardEntryDto> scoreboard(@PathVariable Long id) {
        return gridPlayService.getScoreboard(id);
    }

    @GetMapping("/archive")
    public List<GridSummaryDto> archive(Authentication authentication) {
        return gridPlayService.findArchive(authentication.getName());
    }

    @GetMapping("/{id}/play")
    public GridPlayStateDto play(@PathVariable Long id, Authentication authentication) {
        return gridPlayService.getPlayState(id, authentication.getName());
    }

    @GetMapping("/{id}/candidates")
    public List<AthleteDto> candidates(@PathVariable Long id, @RequestParam(required = false) String search) {
        return gridPlayService.searchCandidates(id, search);
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
