package com.quizapp.controller;

import com.quizapp.dto.AdventDto.*;
import com.quizapp.service.AdventService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/** The advent calendar for players - signed-in accounts only (the default rule for everything outside /api/auth). */
@RestController
@RequestMapping("/api/advent")
public class AdventController {

    private final AdventService adventService;

    public AdventController(AdventService adventService) {
        this.adventService = adventService;
    }

    @GetMapping
    public StateDto state(Authentication authentication) {
        return adventService.state(authentication.getName());
    }

    @GetMapping("/days/{day}")
    public DayDetailDto day(@PathVariable int day, Authentication authentication) {
        return adventService.dayDetail(authentication.getName(), day);
    }

    @PutMapping("/days/{day}/entry")
    public DayDetailDto submit(@PathVariable int day, @RequestBody EntryRequest request, Authentication authentication) {
        return adventService.submit(authentication.getName(), day, request);
    }

    @PutMapping("/profile")
    public void profile(@RequestBody ProfileRequest request, Authentication authentication) {
        adventService.setShowName(authentication.getName(), request.showName());
    }

    @GetMapping("/leaderboard")
    public LeaderboardDto leaderboard(Authentication authentication) {
        return adventService.leaderboard(authentication.getName());
    }

    @GetMapping("/wrapped")
    public WrappedDto wrapped(Authentication authentication) {
        return adventService.wrapped(authentication.getName());
    }
}
