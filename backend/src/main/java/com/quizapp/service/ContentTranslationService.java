package com.quizapp.service;

import com.quizapp.exception.ResourceNotFoundException;
import com.quizapp.model.*;
import com.quizapp.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Admin-written Norwegian / English versions of a game board's text (title, theme, clues, ...). The base text
 * stays on the board itself; this holds only the optional per-language overrides, which
 * {@link com.quizapp.config.TranslationResponseAdvice} swaps into player-facing responses. One service for every
 * game: a board is addressed by (type, id) and its translatable fields by name ("title", "hints#0", ...).
 */
@Service
public class ContentTranslationService {

    public static final List<String> LANGS = List.of("no", "en");
    private static final int MAX_LENGTH = 2000;

    private final ContentTranslationRepository repository;
    private final GridRepository gridRepository;
    private final LineupRepository lineupRepository;
    private final ImposterGridRepository imposterGridRepository;
    private final BullseyeQuestionRepository bullseyeQuestionRepository;
    private final TensionQuestionRepository tensionQuestionRepository;
    private final FiveOhOneCategoryRepository fiveOhOneCategoryRepository;
    private final FlashbackYearRepository flashbackYearRepository;
    private final PenaltyShootoutRepository penaltyShootoutRepository;

    public ContentTranslationService(ContentTranslationRepository repository,
                                     GridRepository gridRepository,
                                     LineupRepository lineupRepository,
                                     ImposterGridRepository imposterGridRepository,
                                     BullseyeQuestionRepository bullseyeQuestionRepository,
                                     TensionQuestionRepository tensionQuestionRepository,
                                     FiveOhOneCategoryRepository fiveOhOneCategoryRepository,
                                     FlashbackYearRepository flashbackYearRepository,
                                     PenaltyShootoutRepository penaltyShootoutRepository) {
        this.repository = repository;
        this.gridRepository = gridRepository;
        this.lineupRepository = lineupRepository;
        this.imposterGridRepository = imposterGridRepository;
        this.bullseyeQuestionRepository = bullseyeQuestionRepository;
        this.tensionQuestionRepository = tensionQuestionRepository;
        this.fiveOhOneCategoryRepository = fiveOhOneCategoryRepository;
        this.flashbackYearRepository = flashbackYearRepository;
        this.penaltyShootoutRepository = penaltyShootoutRepository;
    }

    /** What the admin editor shows: every translatable field with its base text and any saved versions. */
    public static class FieldView {
        private final String key;
        private final String base;
        private final Map<String, String> values;

        FieldView(String key, String base, Map<String, String> values) {
            this.key = key;
            this.base = base;
            this.values = values;
        }

        public String getKey() { return key; }
        public String getBase() { return base; }
        public Map<String, String> getValues() { return values; }
    }

    @Transactional(readOnly = true)
    public List<FieldView> getEditor(String type, Long id) {
        Map<String, String> base = baseFields(type, id);
        Map<String, Map<String, String>> saved = new HashMap<>();
        for (ContentTranslation t : repository.findByEntityTypeAndEntityId(type, id)) {
            saved.computeIfAbsent(t.getFieldName(), k -> new HashMap<>()).put(t.getLang(), t.getContent());
        }
        List<FieldView> out = new ArrayList<>();
        for (Map.Entry<String, String> e : base.entrySet()) {
            Map<String, String> values = new LinkedHashMap<>();
            for (String lang : LANGS) values.put(lang, saved.getOrDefault(e.getKey(), Map.of()).getOrDefault(lang, ""));
            out.add(new FieldView(e.getKey(), e.getValue() == null ? "" : e.getValue(), values));
        }
        return out;
    }

    /** values: field -> lang -> text. A blank text removes that version (the base text is used again). */
    @Transactional
    public void save(String type, Long id, Map<String, Map<String, String>> values) {
        Map<String, String> allowed = baseFields(type, id);
        Map<String, ContentTranslation> existing = new HashMap<>();
        for (ContentTranslation t : repository.findByEntityTypeAndEntityId(type, id)) {
            existing.put(t.getFieldName() + "|" + t.getLang(), t);
        }
        for (Map.Entry<String, Map<String, String>> field : values.entrySet()) {
            if (!allowed.containsKey(field.getKey())) {
                throw new IllegalArgumentException("Unknown translation field: " + field.getKey());
            }
            for (Map.Entry<String, String> lang : field.getValue().entrySet()) {
                if (!LANGS.contains(lang.getKey())) {
                    throw new IllegalArgumentException("Unknown language: " + lang.getKey());
                }
                String text = lang.getValue() == null ? "" : lang.getValue().trim();
                if (text.length() > MAX_LENGTH) {
                    throw new IllegalArgumentException("A translation can be at most " + MAX_LENGTH + " characters.");
                }
                ContentTranslation row = existing.get(field.getKey() + "|" + lang.getKey());
                if (text.isEmpty()) {
                    if (row != null) repository.delete(row);
                } else if (row != null) {
                    row.setContent(text);
                } else {
                    repository.save(new ContentTranslation(type, id, field.getKey(), lang.getKey(), text));
                }
            }
        }
    }

    /** field name -> base text, in the order the editor lists them. Throws if the board doesn't exist. */
    private Map<String, String> baseFields(String type, Long id) {
        Map<String, String> f = new LinkedHashMap<>();
        switch (type) {
            case ContentTranslation.TYPE_GRID -> {
                Grid g = gridRepository.findById(id).orElseThrow(() -> notFound(type, id));
                f.put("title", g.getTitle());
                f.put("theme", g.getTheme());
            }
            case ContentTranslation.TYPE_LINEUP -> {
                Lineup l = lineupRepository.findById(id).orElseThrow(() -> notFound(type, id));
                f.put("title", l.getTitle());
                f.put("competition", l.getCompetition());
            }
            case ContentTranslation.TYPE_IMPOSTER -> {
                ImposterGrid g = imposterGridRepository.findById(id).orElseThrow(() -> notFound(type, id));
                f.put("title", g.getTitle());
                f.put("description", g.getDescription());
            }
            case ContentTranslation.TYPE_BULLSEYE -> {
                BullseyeQuestion q = bullseyeQuestionRepository.findById(id).orElseThrow(() -> notFound(type, id));
                f.put("title", q.getTitle());
                f.put("statLabel", q.getStatLabel());
            }
            case ContentTranslation.TYPE_TENSION -> {
                TensionQuestion q = tensionQuestionRepository.findById(id).orElseThrow(() -> notFound(type, id));
                f.put("title", q.getTitle());
                f.put("source", q.getSource());
                f.put("tiebreaker", q.getTiebreaker());
            }
            case ContentTranslation.TYPE_FIVEOHONE -> {
                FiveOhOneCategory c = fiveOhOneCategoryRepository.findById(id).orElseThrow(() -> notFound(type, id));
                f.put("title", c.getTitle());
                f.put("description", c.getDescription());
            }
            case ContentTranslation.TYPE_FLASHBACK -> {
                FlashbackYear y = flashbackYearRepository.findById(id).orElseThrow(() -> notFound(type, id));
                List<String> hints = y.getHints();
                for (int i = 0; i < hints.size(); i++) f.put("hints#" + i, hints.get(i));
            }
            case ContentTranslation.TYPE_SHOOTOUT -> {
                PenaltyShootout s = penaltyShootoutRepository.findById(id).orElseThrow(() -> notFound(type, id));
                f.put("title", s.getTitle());
                f.put("competition", s.getCompetition());
            }
            default -> throw new IllegalArgumentException("Unknown content type: " + type);
        }
        return f;
    }

    private static ResourceNotFoundException notFound(String type, Long id) {
        return new ResourceNotFoundException("Fant ikke innhold (" + type + ") med id " + id);
    }
}
