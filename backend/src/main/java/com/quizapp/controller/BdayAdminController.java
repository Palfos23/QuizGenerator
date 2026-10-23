package com.quizapp.controller;

import com.quizapp.dto.*;
import com.quizapp.service.BdayQuizService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// The host's side of the birthday quiz: players, the questions, and fixing a score by hand. Under /api/admin/**,
// so it needs the real admin login (SecurityConfig) - only the site's admin can reach any of it.
@RestController
@RequestMapping("/api/admin/bday")
public class BdayAdminController {

    private final BdayQuizService quizService;

    public BdayAdminController(BdayQuizService quizService) {
        this.quizService = quizService;
    }

    @GetMapping("/guests")
    public List<BdayGuestDto> guests() {
        return quizService.listAllGuests();
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

    @PutMapping("/guests/{id}/score")
    public BdayGuestDto setScore(@PathVariable Long id, @Valid @RequestBody BdayScoreRequest request) {
        return quizService.setScore(id, request.getScore());
    }

    @PostMapping("/guests/{id}/reset")
    public BdayGuestDto resetGuest(@PathVariable Long id) {
        return quizService.resetGuest(id);
    }

    @GetMapping("/guests/{id}/answers")
    public List<BdayAnswerViewDto> answers(@PathVariable Long id) {
        return quizService.guestAnswers(id);
    }

    @GetMapping("/questions")
    public List<BdayQuestionAdminDto> questions() {
        return quizService.listQuestions();
    }

    @PostMapping("/questions")
    public ResponseEntity<BdayQuestionAdminDto> createQuestion(@Valid @RequestBody BdayQuestionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(quizService.createQuestion(request));
    }

    @PutMapping("/questions/{id}")
    public BdayQuestionAdminDto updateQuestion(@PathVariable Long id, @Valid @RequestBody BdayQuestionRequest request) {
        return quizService.updateQuestion(id, request);
    }

    @DeleteMapping("/questions/{id}")
    public ResponseEntity<Void> deleteQuestion(@PathVariable Long id) {
        quizService.deleteQuestion(id);
        return ResponseEntity.noContent().build();
    }
}
