package com.quizapp.controller;

import com.quizapp.dto.FlashbackYearDto;
import com.quizapp.service.FlashbackPlayService;
import com.quizapp.service.PlayAccessService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/flashback")
public class FlashbackController {

    private final FlashbackPlayService flashbackPlayService;
    private final PlayAccessService playAccessService;

    public FlashbackController(FlashbackPlayService flashbackPlayService, PlayAccessService playAccessService) {
        this.flashbackPlayService = flashbackPlayService;
        this.playAccessService = playAccessService;
    }

    /** Random round-start "choose one of 3" picker - see FlashbackPlayService.getRoundChoices. */
    @GetMapping("/round-choices")
    public List<FlashbackYearDto> roundChoices(
            @RequestParam(defaultValue = "3") int count,
            @RequestParam(required = false) List<Long> excludeIds,
            Authentication authentication) {
        playAccessService.requireFlashbackAccess(authentication);
        return flashbackPlayService.getRoundChoices(count, excludeIds == null ? Collections.emptyList() : excludeIds);
    }
}
