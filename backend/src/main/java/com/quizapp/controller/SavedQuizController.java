package com.quizapp.controller;

import com.quizapp.dto.QuizDto;
import com.quizapp.dto.SavedQuizSummaryDto;
import com.quizapp.service.SavedQuizService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * "My Quizzes": a user explicitly saves a finalized quiz (nothing is saved just from
 * generating one), and can come back later to view or re-download it. Every endpoint here
 * is scoped to the calling user via their JWT subject (their email) - there's no way to
 * list or fetch another user's saved quizzes.
 */
@RestController
@RequestMapping("/api/quiz/saved")
public class SavedQuizController {

    private final SavedQuizService savedQuizService;

    public SavedQuizController(SavedQuizService savedQuizService) {
        this.savedQuizService = savedQuizService;
    }

    @PostMapping
    public ResponseEntity<QuizDto> save(@Valid @RequestBody QuizDto quiz, Authentication authentication) {
        QuizDto saved = savedQuizService.save(authentication.getName(), quiz);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping
    public List<SavedQuizSummaryDto> list(Authentication authentication) {
        return savedQuizService.listForUser(authentication.getName());
    }

    @GetMapping("/{id}")
    public QuizDto getOne(@PathVariable Long id, Authentication authentication) {
        return savedQuizService.getOne(authentication.getName(), id);
    }

    /** Overwrites a previously saved quiz - same id, updated content (e.g. after re-editing it). */
    @PutMapping("/{id}")
    public QuizDto update(@PathVariable Long id, @Valid @RequestBody QuizDto quiz, Authentication authentication) {
        return savedQuizService.update(authentication.getName(), id, quiz);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication authentication) {
        savedQuizService.delete(authentication.getName(), id);
        return ResponseEntity.noContent().build();
    }
}
