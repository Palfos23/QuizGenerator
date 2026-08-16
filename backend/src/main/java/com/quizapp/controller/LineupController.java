package com.quizapp.controller;

import com.quizapp.dto.AthleteDto;
import com.quizapp.dto.LineupGuessRequest;
import com.quizapp.dto.LineupGuessResultDto;
import com.quizapp.dto.LineupPlayStateDto;
import com.quizapp.dto.LineupSummaryDto;
import com.quizapp.service.LineupPlayService;
import jakarta.validation.Valid;
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
    public List<LineupSummaryDto> findAll() {
        return lineupPlayService.findAll();
    }

    @GetMapping("/{id}/play")
    public LineupPlayStateDto play(@PathVariable Long id) {
        return lineupPlayService.getStartState(id);
    }

    @GetMapping("/{id}/candidates")
    public List<AthleteDto> candidates(@PathVariable Long id, @RequestParam(required = false) String search) {
        return lineupPlayService.searchCandidates(id, search);
    }

    @PostMapping("/{id}/guess")
    public LineupGuessResultDto guess(@PathVariable Long id, @Valid @RequestBody LineupGuessRequest request) {
        return lineupPlayService.guess(id, request);
    }

    @GetMapping("/{id}/reveal")
    public Map<Long, String> reveal(@PathVariable Long id) {
        return lineupPlayService.reveal(id);
    }
}
