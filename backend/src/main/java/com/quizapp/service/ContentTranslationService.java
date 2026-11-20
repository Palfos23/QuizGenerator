package com.quizapp.service;

import com.quizapp.exception.ResourceNotFoundException;
import com.quizapp.model.*;
import com.quizapp.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
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

    @PersistenceContext
    private EntityManager entityManager;

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
        return fieldsOf(type, load(type, id));
    }

    private Object load(String type, Long id) {
        return switch (type) {
            case ContentTranslation.TYPE_GRID -> gridRepository.findById(id).orElseThrow(() -> notFound(type, id));
            case ContentTranslation.TYPE_LINEUP -> lineupRepository.findById(id).orElseThrow(() -> notFound(type, id));
            case ContentTranslation.TYPE_IMPOSTER -> imposterGridRepository.findById(id).orElseThrow(() -> notFound(type, id));
            case ContentTranslation.TYPE_BULLSEYE -> bullseyeQuestionRepository.findById(id).orElseThrow(() -> notFound(type, id));
            case ContentTranslation.TYPE_TENSION -> tensionQuestionRepository.findById(id).orElseThrow(() -> notFound(type, id));
            case ContentTranslation.TYPE_FIVEOHONE -> fiveOhOneCategoryRepository.findById(id).orElseThrow(() -> notFound(type, id));
            case ContentTranslation.TYPE_FLASHBACK -> flashbackYearRepository.findById(id).orElseThrow(() -> notFound(type, id));
            case ContentTranslation.TYPE_SHOOTOUT -> penaltyShootoutRepository.findById(id).orElseThrow(() -> notFound(type, id));
            default -> throw new IllegalArgumentException("Unknown content type: " + type);
        };
    }

    private Map<String, String> fieldsOf(String type, Object board) {
        Map<String, String> f = new LinkedHashMap<>();
        switch (type) {
            case ContentTranslation.TYPE_GRID -> {
                Grid g = (Grid) board;
                f.put("title", g.getTitle());
                f.put("theme", g.getTheme());
            }
            case ContentTranslation.TYPE_LINEUP -> {
                Lineup l = (Lineup) board;
                f.put("title", l.getTitle());
                f.put("competition", l.getCompetition());
            }
            case ContentTranslation.TYPE_IMPOSTER -> {
                ImposterGrid g = (ImposterGrid) board;
                f.put("title", g.getTitle());
                f.put("description", g.getDescription());
            }
            case ContentTranslation.TYPE_BULLSEYE -> {
                BullseyeQuestion q = (BullseyeQuestion) board;
                f.put("title", q.getTitle());
                f.put("statLabel", q.getStatLabel());
            }
            case ContentTranslation.TYPE_TENSION -> {
                TensionQuestion q = (TensionQuestion) board;
                f.put("title", q.getTitle());
                f.put("source", q.getSource());
                f.put("tiebreaker", q.getTiebreaker());
            }
            case ContentTranslation.TYPE_FIVEOHONE -> {
                FiveOhOneCategory c = (FiveOhOneCategory) board;
                f.put("title", c.getTitle());
                f.put("description", c.getDescription());
            }
            case ContentTranslation.TYPE_FLASHBACK -> {
                FlashbackYear y = (FlashbackYear) board;
                List<String> hints = y.getHints();
                for (int i = 0; i < hints.size(); i++) f.put("hints#" + i, hints.get(i));
            }
            case ContentTranslation.TYPE_SHOOTOUT -> {
                PenaltyShootout s = (PenaltyShootout) board;
                f.put("title", s.getTitle());
                f.put("competition", s.getCompetition());
            }
            default -> throw new IllegalArgumentException("Unknown content type: " + type);
        }
        return f;
    }

    // ---- Bulk export / import (so a whole library can be translated outside the app, then loaded back) ----

    public static final List<String> TYPES = List.of(
            ContentTranslation.TYPE_GRID, ContentTranslation.TYPE_LINEUP, ContentTranslation.TYPE_IMPOSTER,
            ContentTranslation.TYPE_BULLSEYE, ContentTranslation.TYPE_TENSION, ContentTranslation.TYPE_FIVEOHONE,
            ContentTranslation.TYPE_FLASHBACK, ContentTranslation.TYPE_SHOOTOUT);

    public record ExportField(String key, String base, String no, String en) {}

    /** context = a title to read the fields against (the year's title for Flashback hints, etc). */
    public record ExportItem(String type, Long id, String context, List<ExportField> fields) {}

    public record ImportField(String no, String en) {}

    public record ImportItem(String type, Long id, Map<String, ImportField> fields) {}

    public record ImportResult(int saved, int skippedExisting, int unknownBoards, int unknownFields, List<String> problems) {}

    // Text columns per game, read with a plain column query: the boards themselves carry every entry they hold
    // (hundreds of rows each), none of which an export needs.
    private static final Map<String, List<String>> TEXT_FIELDS = Map.of(
            ContentTranslation.TYPE_GRID, List.of("title", "theme"),
            ContentTranslation.TYPE_LINEUP, List.of("title", "competition"),
            ContentTranslation.TYPE_IMPOSTER, List.of("title", "description"),
            ContentTranslation.TYPE_BULLSEYE, List.of("title", "statLabel"),
            ContentTranslation.TYPE_TENSION, List.of("title", "source", "tiebreaker"),
            ContentTranslation.TYPE_FIVEOHONE, List.of("title", "description"),
            ContentTranslation.TYPE_SHOOTOUT, List.of("title", "competition"));

    private static final Map<String, String> ENTITY_NAMES = Map.of(
            ContentTranslation.TYPE_GRID, "Grid",
            ContentTranslation.TYPE_LINEUP, "Lineup",
            ContentTranslation.TYPE_IMPOSTER, "ImposterGrid",
            ContentTranslation.TYPE_BULLSEYE, "BullseyeQuestion",
            ContentTranslation.TYPE_TENSION, "TensionQuestion",
            ContentTranslation.TYPE_FIVEOHONE, "FiveOhOneCategory",
            ContentTranslation.TYPE_SHOOTOUT, "PenaltyShootout");

    private record BoardText(Long id, Map<String, String> fields) {}

    private List<BoardText> everyBoard(String type) {
        List<BoardText> out = new ArrayList<>();
        if (type.equals(ContentTranslation.TYPE_FLASHBACK)) {
            for (FlashbackYear y : flashbackYearRepository.findAll()) out.add(new BoardText(y.getId(), fieldsOf(type, y)));
            return out;
        }
        List<String> names = TEXT_FIELDS.get(type);
        String select = "e.id, " + String.join(", ", names.stream().map(n -> "e." + n).toList());
        List<Object[]> rows = entityManager.createQuery(
                "select " + select + " from " + ENTITY_NAMES.get(type) + " e order by e.id", Object[].class).getResultList();
        for (Object[] row : rows) {
            Map<String, String> fields = new LinkedHashMap<>();
            for (int i = 0; i < names.size(); i++) fields.put(names.get(i), (String) row[i + 1]);
            out.add(new BoardText((Long) row[0], fields));
        }
        return out;
    }

    /**
     * Every board's text with whatever versions already exist. With onlyMissing, fields that already have both a
     * Norwegian and an English version are left out (and boards with nothing left to do are dropped).
     */
    @Transactional(readOnly = true)
    public List<ExportItem> exportAll(boolean onlyMissing) {
        Map<String, Map<String, String>> saved = new HashMap<>(); // "TYPE|id|field" -> lang -> text
        for (ContentTranslation t : repository.findAll()) {
            saved.computeIfAbsent(t.getEntityType() + "|" + t.getEntityId() + "|" + t.getFieldName(), k -> new HashMap<>())
                    .put(t.getLang(), t.getContent());
        }
        List<ExportItem> out = new ArrayList<>();
        for (String type : TYPES) {
            for (BoardText board : everyBoard(type)) {
                Long id = board.id();
                Map<String, String> base = board.fields();
                List<ExportField> fields = new ArrayList<>();
                for (Map.Entry<String, String> e : base.entrySet()) {
                    if (e.getValue() == null || e.getValue().isBlank()) continue;
                    Map<String, String> have = saved.getOrDefault(type + "|" + id + "|" + e.getKey(), Map.of());
                    String no = have.getOrDefault("no", "");
                    String en = have.getOrDefault("en", "");
                    if (onlyMissing && !no.isEmpty() && !en.isEmpty()) continue;
                    fields.add(new ExportField(e.getKey(), e.getValue(), no, en));
                }
                if (fields.isEmpty()) continue;
                String context = base.get("title");
                out.add(new ExportItem(type, id, context, fields));
            }
        }
        return out;
    }

    /**
     * Loads translated text back in. Blank texts are ignored; a version that already exists is kept unless
     * overwrite is set, so re-importing a file never clobbers something an admin wrote by hand.
     */
    @Transactional
    public ImportResult importAll(List<ImportItem> items, boolean overwrite) {
        Map<String, ContentTranslation> existing = new HashMap<>();
        for (ContentTranslation t : repository.findAll()) {
            existing.put(t.getEntityType() + "|" + t.getEntityId() + "|" + t.getFieldName() + "|" + t.getLang(), t);
        }
        int saved = 0, skipped = 0, unknownBoards = 0, unknownFields = 0;
        List<String> problems = new ArrayList<>();
        for (ImportItem item : items) {
            String type = item.type() == null ? "" : item.type().toUpperCase();
            if (!TYPES.contains(type) || item.id() == null || item.fields() == null) { unknownBoards++; continue; }
            Map<String, String> allowed;
            try {
                allowed = baseFields(type, item.id());
            } catch (ResourceNotFoundException e) {
                unknownBoards++;
                continue;
            }
            for (Map.Entry<String, ImportField> field : item.fields().entrySet()) {
                if (!allowed.containsKey(field.getKey()) || field.getValue() == null) { unknownFields++; continue; }
                Map<String, String> byLang = new LinkedHashMap<>();
                byLang.put("no", field.getValue().no());
                byLang.put("en", field.getValue().en());
                for (Map.Entry<String, String> lang : byLang.entrySet()) {
                    String text = lang.getValue() == null ? "" : lang.getValue().trim();
                    if (text.isEmpty()) continue;
                    if (text.length() > MAX_LENGTH) {
                        problems.add(type + " " + item.id() + " " + field.getKey() + ": longer than " + MAX_LENGTH + " characters");
                        continue;
                    }
                    String key = type + "|" + item.id() + "|" + field.getKey() + "|" + lang.getKey();
                    ContentTranslation row = existing.get(key);
                    if (row != null) {
                        if (!overwrite) { skipped++; continue; }
                        row.setContent(text);
                    } else {
                        existing.put(key, repository.save(new ContentTranslation(type, item.id(), field.getKey(), lang.getKey(), text)));
                    }
                    saved++;
                }
            }
        }
        return new ImportResult(saved, skipped, unknownBoards, unknownFields, problems);
    }

    private static ResourceNotFoundException notFound(String type, Long id) {
        return new ResourceNotFoundException("Fant ikke innhold (" + type + ") med id " + id);
    }
}
