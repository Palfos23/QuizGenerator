package com.quizapp.controller;

import com.quizapp.dto.LeagueDtos.AdminLeagueOverview;
import com.quizapp.dto.LeagueDtos.LeagueDetail;
import com.quizapp.service.LeagueService;
import org.springframework.web.bind.annotation.*;

/** The admin's look at the private leagues: totals, every league, any league read-only. ADMIN only (/api/admin/**). */
@RestController
@RequestMapping("/api/admin/leagues")
public class AdminLeagueController {

    private final LeagueService leagueService;

    public AdminLeagueController(LeagueService leagueService) {
        this.leagueService = leagueService;
    }

    @GetMapping
    public AdminLeagueOverview overview() {
        return leagueService.adminOverview();
    }

    @GetMapping("/{id}")
    public LeagueDetail detail(@PathVariable Long id, @RequestParam(required = false) Integer season) {
        return leagueService.adminDetail(id, season);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        leagueService.adminDelete(id);
    }
}
