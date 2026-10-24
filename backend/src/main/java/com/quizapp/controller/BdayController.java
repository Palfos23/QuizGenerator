package com.quizapp.controller;

import com.quizapp.dto.BdayGuestDto;
import com.quizapp.dto.BdayQuizDto;
import com.quizapp.dto.BdayResultDto;
import com.quizapp.dto.BdaySubmitRequest;
import com.quizapp.dto.BdayNightDto;
import com.quizapp.service.BdayNightService;
import com.quizapp.service.BdayQuizService;
import com.quizapp.service.BdayStandingsService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// The one-off birthday quiz, as the guests see it: open to everyone, no login and no PIN - a guest picks their
// name from the list on their own phone and answers. Nothing here reveals a correct answer. Everything the host
// does (players, questions, score fixes) is in BdayAdminController, behind the real admin login.
@RestController
@RequestMapping("/api/bday")
public class BdayController {

    private final BdayQuizService quizService;
    private final BdayNightService nightService;
    private final BdayStandingsService standingsService;

    public BdayController(BdayQuizService quizService, BdayNightService nightService, BdayStandingsService standingsService) {
        this.quizService = quizService;
        this.nightService = nightService;
        this.standingsService = standingsService;
    }

    /** The names still waiting to play. */
    @GetMapping("/guests")
    public List<BdayGuestDto> guests() {
        return quizService.listPendingGuests();
    }

    @GetMapping("/quiz/{guestId}")
    public BdayQuizDto quiz(@PathVariable Long guestId) {
        return quizService.getQuiz(guestId);
    }

    @PostMapping("/quiz/{guestId}/submit")
    public BdayResultDto submit(@PathVariable Long guestId, @Valid @RequestBody BdaySubmitRequest request) {
        return quizService.submitAnswers(guestId, request);
    }

    @GetMapping("/leaderboard")
    public List<BdayGuestDto> leaderboard() {
        return quizService.leaderboard();
    }

    /** The combined board: every guest's total and where it comes from, the music teams, and the Mario Kart ranking. */
    @GetMapping("/standings")
    public BdayNightDto.Standings standings() {
        return standingsService.standings();
    }

    /** Who can still enter a Mario Kart time, and the ranking so far. */
    @GetMapping("/mariokart")
    public BdayNightDto.MarioKartPublic marioKart() {
        return nightService.marioKartPublic();
    }

    /** A guest types their own best lap - once, and only while the host has registration open. */
    @PostMapping("/mariokart/{guestId}/time")
    public BdayNightDto.LapResult enterTime(@PathVariable Long guestId, @Valid @RequestBody BdayNightDto.TimeRequest request) {
        return nightService.enterOwnTime(guestId, request.time());
    }
}
