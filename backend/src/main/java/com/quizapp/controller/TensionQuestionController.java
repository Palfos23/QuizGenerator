package com.quizapp.controller;

import com.quizapp.dto.TensionQuestionDto;
import com.quizapp.service.TensionQuestionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tension/questions")
public class TensionQuestionController {

    private final TensionQuestionService questionService;

    public TensionQuestionController(TensionQuestionService questionService) {
        this.questionService = questionService;
    }

    @GetMapping("/random")
    public List<TensionQuestionDto> random(
            @RequestParam(defaultValue = "5") int count,
            @RequestParam(required = false) String category) {
        return questionService.getRandom(count, category);
    }

    @GetMapping("/categories")
    public List<String> categories() {
        return questionService.getDistinctMainCategories();
    }
}
