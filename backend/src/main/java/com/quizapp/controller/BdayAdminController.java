package com.quizapp.controller;

import com.quizapp.dto.*;
import com.quizapp.service.BdayQuizService;
import com.quizapp.service.ImageStorageService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

// The host's side of the birthday quiz: players, the questions, and fixing a score by hand. Gated by the quiz's own PIN
// (BdayPinAuthFilter), not by the site's admin role - some admins are guests at the party, and this side shows the answers.
@RestController
@RequestMapping("/api/bday/admin")
public class BdayAdminController {

    private final BdayQuizService quizService;
    private final ImageStorageService imageStorageService;

    public BdayAdminController(BdayQuizService quizService, ImageStorageService imageStorageService) {
        this.quizService = quizService;
        this.imageStorageService = imageStorageService;
    }

    /** A photo for a picture question - the same storage the site's admin uploads use, but behind the quiz's PIN. */
    @PostMapping("/images")
    public Map<String, String> uploadImage(@RequestParam("file") MultipartFile file) throws IOException {
        return Map.of("url", imageStorageService.upload(file.getBytes()));
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
