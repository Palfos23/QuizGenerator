package com.quizapp.controller;

import com.quizapp.dto.BdayGuestDto;
import com.quizapp.dto.BdayQuizDto;
import com.quizapp.dto.BdayResultDto;
import com.quizapp.dto.BdaySubmitRequest;
import com.quizapp.service.BdayQuizService;
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

    public BdayController(BdayQuizService quizService) {
        this.quizService = quizService;
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
}
