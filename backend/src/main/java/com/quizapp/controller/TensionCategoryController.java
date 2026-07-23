package com.quizapp.controller;

import com.quizapp.service.TensionCategoryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tension/categories")
public class TensionCategoryController {

    private final TensionCategoryService categoryService;

    public TensionCategoryController(TensionCategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping("/{name}/options")
    public List<String> options(@PathVariable String name) {
        return categoryService.getOptions(name);
    }
}
