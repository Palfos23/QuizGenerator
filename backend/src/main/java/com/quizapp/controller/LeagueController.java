package com.quizapp.controller;

import com.quizapp.dto.LeagueDtos.*;
import com.quizapp.service.LeagueService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Private daily-quiz leagues (see LeagueService). Player accounts only - SecurityConfig's default rule. */
@RestController
@RequestMapping("/api/leagues")
public class LeagueController {

    private final LeagueService leagueService;

    public LeagueController(LeagueService leagueService) {
        this.leagueService = leagueService;
    }

    @GetMapping
    public List<LeagueCard> mine(Authentication auth) {
        return leagueService.myLeagues(auth.getName());
    }

    @PostMapping
    public LeagueDetail create(@RequestBody CreateLeagueRequest request, Authentication auth) {
        return leagueService.create(auth.getName(), request);
    }

    @GetMapping("/code/{code}")
    public JoinPreview preview(@PathVariable String code, Authentication auth) {
        return leagueService.preview(auth.getName(), code);
    }

    @PostMapping("/join")
    public LeagueDetail join(@RequestBody JoinLeagueRequest request, Authentication auth) {
        return leagueService.join(auth.getName(), request.code());
    }

    @GetMapping("/{id}")
    public LeagueDetail detail(@PathVariable Long id, @RequestParam(required = false) Integer season, Authentication auth) {
        return leagueService.detail(auth.getName(), id, season);
    }

    @PostMapping("/{id}/leave")
    public void leave(@PathVariable Long id, Authentication auth) {
        leagueService.leave(auth.getName(), id);
    }

    @DeleteMapping("/{id}/members/{userId}")
    public LeagueDetail removeMember(@PathVariable Long id, @PathVariable Long userId, Authentication auth) {
        return leagueService.removeMember(auth.getName(), id, userId);
    }

    @PostMapping("/{id}/seasons")
    public LeagueDetail newSeason(@PathVariable Long id, @RequestBody SeasonSettings settings, Authentication auth) {
        return leagueService.newSeason(auth.getName(), id, settings);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id, Authentication auth) {
        leagueService.delete(auth.getName(), id);
    }
}
