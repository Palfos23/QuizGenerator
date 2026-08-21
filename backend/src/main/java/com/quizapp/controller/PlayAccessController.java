package com.quizapp.controller;

import com.quizapp.service.PlayAccessService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// A single upfront check each game's *View.vue calls before showing any of
// its setup UI - so a restricted user sees only the "you don't have access"
// message and never gets to interact with player names, mode choice, etc.
// Throws (via PlayAccessService, mapped to 403 by GlobalExceptionHandler) if
// denied; a plain 200 with no body means access is fine.
@RestController
@RequestMapping("/api/play-access")
public class PlayAccessController {

    private final PlayAccessService playAccessService;

    public PlayAccessController(PlayAccessService playAccessService) {
        this.playAccessService = playAccessService;
    }

    @GetMapping("/{game}")
    public void check(@PathVariable String game, Authentication authentication) {
        playAccessService.requireAccessForKey(authentication, game);
    }
}
