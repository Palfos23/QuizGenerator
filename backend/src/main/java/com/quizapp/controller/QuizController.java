package com.quizapp.controller;

import com.quizapp.dto.*;
import com.quizapp.service.PdfService;
import com.quizapp.service.QuestionService;
import com.quizapp.service.QuizService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/quiz")
public class QuizController {

    private final QuizService quizService;
    private final PdfService pdfService;
    private final QuestionService questionService;

    public QuizController(QuizService quizService, PdfService pdfService, QuestionService questionService) {
        this.quizService = quizService;
        this.pdfService = pdfService;
        this.questionService = questionService;
    }

    /** Use case 1 & 2: generate a quiz with N questions, a difficulty and one or more categories. */
    @PostMapping("/generate")
    public QuizDto generate(@Valid @RequestBody QuizGenerateRequest request, org.springframework.security.core.Authentication authentication) {
        return quizService.generate(request, authentication.getName());
    }

    /** Use case 3: discard one question from the draft quiz and get a fresh replacement. */
    @PostMapping("/replace-question")
    public QuestionDto replaceQuestion(@Valid @RequestBody ReplaceQuestionRequest request) {
        return quizService.replaceOne(request);
    }

    /** Add more questions to an in-progress quiz - a new category, or more of one already there. */
    @PostMapping("/add-questions")
    public List<QuestionDto> addQuestions(@Valid @RequestBody AddQuestionsRequest request) {
        return quizService.addQuestions(request);
    }

    /** Categories that have at least one question in the given language, to populate the generator form. */
    @GetMapping("/categories")
    public List<String> categories(@RequestParam(defaultValue = "EN") com.quizapp.model.Language language) {
        return questionService.findCategoriesForLanguage(language);
    }

    /** Search the bank to hand-pick specific questions into a quiz, rather than only a random batch. */
    @GetMapping("/search-questions")
    public List<QuestionDto> searchQuestions(
            @RequestParam(defaultValue = "EN") com.quizapp.model.Language language,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category) {
        return questionService.searchForUser(language, search, category);
    }

    /** Use case 4: download the finalized quiz as a PDF. */
    @PostMapping("/export/pdf")
    public ResponseEntity<byte[]> exportPdf(
            @Valid @RequestBody QuizDto quiz,
            @RequestParam(defaultValue = "true") boolean includeAnswers) {

        byte[] pdf = pdfService.renderQuiz(quiz, includeAnswers);
        String filename = quiz.getTitle().replaceAll("[^a-zA-Z0-9-_]", "_") + ".pdf";

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(pdf);
    }
}
