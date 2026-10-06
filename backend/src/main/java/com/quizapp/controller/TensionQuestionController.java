package com.quizapp.controller;

import com.quizapp.dto.TensionQuestionDto;
import com.quizapp.service.PlayAccessService;
import com.quizapp.service.TensionQuestionService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/tension/questions")
public class TensionQuestionController {

    private final TensionQuestionService questionService;
    private final PlayAccessService playAccessService;

    public TensionQuestionController(TensionQuestionService questionService, PlayAccessService playAccessService) {
        this.questionService = questionService;
        this.playAccessService = playAccessService;
    }

    @GetMapping("/random")
    public List<TensionQuestionDto> random(
            @RequestParam(defaultValue = "5") int count,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) List<String> excludeCategories,
            Authentication authentication) {
        playAccessService.requireTensionAccess(authentication);
        return questionService.getRandom(count, category, excludeCategories == null ? Collections.emptyList() : excludeCategories);
    }

    @GetMapping("/round-choices")
    public List<TensionQuestionDto> roundChoices(
            @RequestParam(defaultValue = "3") int count,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) List<String> excludeCategories,
            @RequestParam(required = false) List<Long> excludeIds,
            Authentication authentication) {
        playAccessService.requireTensionAccess(authentication);
        return questionService.getRoundChoices(count, category,
                excludeCategories == null ? Collections.emptyList() : excludeCategories,
                excludeIds == null ? Collections.emptyList() : excludeIds);
    }

    @GetMapping("/categories")
    public List<String> categories() {
        return questionService.getDistinctMainCategories();
    }

    // For a question whose answersFromSubjects is true - the answer-box
    // autocomplete's Subjects-sourced equivalent of TensionCategoryController's
    // /categories/{name}/options.
    @GetMapping("/subject-options")
    public List<String> subjectOptions(@RequestParam String sport, Authentication authentication) {
        // A guest has no AppUser row to check canPlayTension against - same exemption
        // FiveOhOneCategoryController applies for its in-room lookup: the host already
        // passed this check when creating the room, and this is that guest reading
        // the answer list for a question they're already validly sitting in.
        if (!isGuest(authentication)) {
            playAccessService.requireTensionAccess(authentication);
        }
        return questionService.getSubjectOptions(sport);
    }

    private boolean isGuest(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_GUEST"));
    }
}
