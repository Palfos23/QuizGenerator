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

    /** Every board's text plus existing versions, as JSON to translate outside the app. */
    @GetMapping("/export")
    public List<ContentTranslationService.ExportItem> export(@RequestParam(defaultValue = "true") boolean onlyMissing) {
        return service.exportAll(onlyMissing);
    }

    /** Body: the (translated) export. Existing versions are kept unless overwrite=true. */
    @PostMapping("/import")
    public ContentTranslationService.ImportResult importTranslations(
            @RequestBody List<ContentTranslationService.ImportItem> items,
            @RequestParam(defaultValue = "false") boolean overwrite) {
        return service.importAll(items, overwrite);
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
