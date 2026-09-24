package com.quizapp.controller;

import com.quizapp.dto.BdayAddGuestRequest;
import com.quizapp.dto.BdayGuestDto;
import com.quizapp.dto.BdayQuizDto;
import com.quizapp.dto.BdayResultDto;
import com.quizapp.dto.BdaySubmitRequest;
import com.quizapp.service.BdayQuizService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// One-off birthday-quiz feature - gated by BdayPinAuthFilter (a single shared
// PIN, entirely separate from the real admin/JWT/role system), not by
// Spring Security roles. See the plan doc for context; safe to delete this
// whole file (plus the rest of the Bday* files) after the party.
@RestController
@RequestMapping("/api/bday")
public class BdayController {

    private final BdayQuizService quizService;

    public BdayController(BdayQuizService quizService) {
        this.quizService = quizService;
    }

    @GetMapping("/guests")
    public List<BdayGuestDto> guests() {
        return quizService.listPendingGuests();
    }

    @PostMapping("/guests")
    public ResponseEntity<BdayGuestDto> addGuest(@Valid @RequestBody BdayAddGuestRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(quizService.addGuest(request.getName()));
    }

    @DeleteMapping("/guests/{id}")
    public ResponseEntity<Void> deleteGuest(@PathVariable Long id) {
        quizService.deleteGuest(id);
        return ResponseEntity.noContent().build();
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
