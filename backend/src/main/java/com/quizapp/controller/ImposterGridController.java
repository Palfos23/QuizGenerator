package com.quizapp.controller;

import com.quizapp.dto.ImposterFlipResultDto;
import com.quizapp.dto.ImposterGridSummaryDto;
import com.quizapp.dto.ImposterPlayStateDto;
import com.quizapp.dto.ImposterRevealDto;
import com.quizapp.service.ImposterGridPlayService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/imposter-grids")
public class ImposterGridController {

    private final ImposterGridPlayService playService;

    public ImposterGridController(ImposterGridPlayService playService) {
        this.playService = playService;
    }

    @GetMapping
    public List<ImposterGridSummaryDto> list(@RequestParam(required = false) String sport) {
        return playService.list(sport);
    }

    /** Random Imposter Battle's round-start "choose one of 3" picker - see ImposterGridPlayService.getBattleRoundChoices. */
    @GetMapping("/battle-round-choices")
    public List<ImposterGridSummaryDto> battleRoundChoices(
            @RequestParam(defaultValue = "3") int count,
            @RequestParam(required = false) List<Long> excludeIds) {
        return playService.getBattleRoundChoices(count, excludeIds);
    }

    @GetMapping("/{id}/play")
    public ImposterPlayStateDto getPlayState(@PathVariable Long id) {
        return playService.getPlayState(id);
    }

    @PostMapping("/{id}/tiles/{tileId}/flip")
    public ImposterFlipResultDto flip(@PathVariable Long id, @PathVariable Long tileId) {
        return playService.flip(id, tileId);
    }

    @GetMapping("/{id}/reveal")
    public List<ImposterRevealDto> reveal(@PathVariable Long id) {
        return playService.reveal(id);
    }
}
