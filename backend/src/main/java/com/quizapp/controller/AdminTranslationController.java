package com.quizapp.controller;

import com.quizapp.service.ContentTranslationService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Norwegian / English versions of a board's text, edited next to the board itself in each game's admin page. */
@RestController
@RequestMapping("/api/admin/translations")
public class AdminTranslationController {

    private final ContentTranslationService service;

    public AdminTranslationController(ContentTranslationService service) {
        this.service = service;
    }

    @GetMapping("/{type}/{id}")
    public List<ContentTranslationService.FieldView> get(@PathVariable String type, @PathVariable Long id) {
        return service.getEditor(type.toUpperCase(), id);
    }

    /** Body: { "title": { "no": "...", "en": "..." }, ... } - a blank text removes that version. */
    @PutMapping("/{type}/{id}")
    public List<ContentTranslationService.FieldView> save(@PathVariable String type, @PathVariable Long id,
                                                         @RequestBody Map<String, Map<String, String>> values) {
        String t = type.toUpperCase();
        service.save(t, id, values);
        return service.getEditor(t, id);
    }
}
