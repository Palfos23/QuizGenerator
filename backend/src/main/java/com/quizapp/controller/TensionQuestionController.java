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
}
