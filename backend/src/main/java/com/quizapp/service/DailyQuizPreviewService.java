package com.quizapp.service;

import com.quizapp.dto.DailyQuizPreviewDto;
import com.quizapp.exception.ResourceNotFoundException;
import com.quizapp.model.*;
import com.quizapp.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Lets an admin look at tomorrow's daily quiz before it goes live, and change it: swap a question for another
 * of the same kind, pick a specific replacement, or rule out a whole type (a question category or label) for
 * that day. The quiz is normally created by the first player to ask for it on the day; previewing simply
 * creates it early, and from then on it is the one players get (they can't see it before its date - see
 * DailyQuizService#requireSet). Only tomorrow can be changed: today's quiz may already have answers on it.
 */
@Service
public class DailyQuizPreviewService {

    private static final int HISTORY_DAYS = 4; // the previewed day plus the three before it
    private static final int CANDIDATE_LIMIT = 30;

    private final DailyQuizService quizService;
    private final DailyQuizSetRepository setRepository;
    private final DailyQuizQuestionUseRepository useRepository;
    private final DailyQuizExclusionRepository exclusionRepository;
    private final QuestionRepository questionRepository;
    private final QuestionLabelRepository labelRepository;

    public DailyQuizPreviewService(DailyQuizService quizService,
                                   DailyQuizSetRepository setRepository,
                                   DailyQuizQuestionUseRepository useRepository,
                                   DailyQuizExclusionRepository exclusionRepository,
                                   QuestionRepository questionRepository,
                                   QuestionLabelRepository labelRepository) {
        this.quizService = quizService;
        this.setRepository = setRepository;
        this.useRepository = useRepository;
        this.exclusionRepository = exclusionRepository;
        this.questionRepository = questionRepository;
        this.labelRepository = labelRepository;
    }

    /** The day being previewed: tomorrow, by the server's clock (the same clock that decides which quiz is "today"). */
    public static LocalDate previewDate() {
        return LocalDate.now().plusDays(1);
    }

    @Transactional
    public DailyQuizPreviewDto get() {
        return build(requireSet());
    }

    /** Swaps one question for a random other of the same kind. */
    @Transactional
    public DailyQuizPreviewDto replace(Long questionId) {
        DailyQuizSet set = requireSet();
        int index = indexOf(set, questionId);
        Question old = questionRepository.findById(questionId).orElseThrow(() -> notFound(questionId));
        Question pick = pickReplacement(set, old)
                .orElseThrow(() -> new IllegalArgumentException("Fant ingen andre spørsmål av samme type å bytte til."));
        put(set, index, pick);
        return build(set);
    }

    /** Swaps one question for a specific one chosen by the admin. */
    @Transactional
    public DailyQuizPreviewDto choose(Long questionId, Long newQuestionId) {
        DailyQuizSet set = requireSet();
        int index = indexOf(set, questionId);
        Question old = questionRepository.findById(questionId).orElseThrow(() -> notFound(questionId));
        Question wanted = questionRepository.findById(newQuestionId).orElseThrow(() -> notFound(newQuestionId));
        if (wanted.getLanguage() != Language.NO) {
            throw new IllegalArgumentException("Den daglige quizen bruker bare norske spørsmål.");
        }
        if (set.getQuestionIds().contains(newQuestionId)) {
            throw new IllegalArgumentException("Det spørsmålet er allerede med i quizen.");
        }
        if (!slotOf(wanted).equals(slotOf(old))) {
            throw new IllegalArgumentException("Dette er en spesialplass (Logo eller År) - velg et spørsmål av samme type.");
        }
        if (exclusionRepository.findByQuizDate(set.getQuizDate()).stream().anyMatch(e -> e.matches(wanted))) {
            throw new IllegalArgumentException("Det spørsmålet er av en type du har utelatt for denne dagen.");
        }
        put(set, index, wanted);
        return build(set);
    }

    /** Rules out a category or label for the day, and re-draws every question in the quiz that it covers. */
    @Transactional
    public DailyQuizPreviewDto addExclusion(String kind, String value) {
        String k = kind == null ? "" : kind.trim().toUpperCase();
        String v = value == null ? "" : value.trim();
        if (!k.equals(DailyQuizExclusion.CATEGORY) && !k.equals(DailyQuizExclusion.LABEL)) {
            throw new IllegalArgumentException("Ukjent type: " + kind);
        }
        if (v.isEmpty()) {
            throw new IllegalArgumentException("Velg en kategori eller merkelapp.");
        }
        DailyQuizSet set = requireSet();
        LocalDate date = set.getQuizDate();
        boolean known = exclusionRepository.findByQuizDate(date).stream()
                .anyMatch(e -> e.getKind().equals(k) && e.getValue().equalsIgnoreCase(v));
        if (!known) exclusionRepository.save(new DailyQuizExclusion(date, k, v));

        // Re-draw what's now excluded. A question with no possible replacement stays (and is flagged in the
        // result) rather than leaving the quiz short.
        List<DailyQuizExclusion> exclusions = exclusionRepository.findByQuizDate(date);
        for (Long id : new ArrayList<>(set.getQuestionIds())) {
            Question q = questionRepository.findById(id).orElse(null);
            if (q == null || exclusions.stream().noneMatch(e -> e.matches(q))) continue;
            int index = set.getQuestionIds().indexOf(id);
            Optional<Question> pick = pickReplacement(set, q);
            if (pick.isPresent()) put(set, index, pick.get());
        }
        return build(set);
    }

    /** Lifts a rule. The quiz is left as it is - this only stops the type being ruled out for later swaps. */
    @Transactional
    public DailyQuizPreviewDto removeExclusion(String kind, String value) {
        DailyQuizSet set = requireSet();
        exclusionRepository.findByQuizDate(set.getQuizDate()).stream()
                .filter(e -> e.getKind().equalsIgnoreCase(kind == null ? "" : kind.trim()) && e.getValue().equalsIgnoreCase(value == null ? "" : value.trim()))
                .forEach(exclusionRepository::delete);
        return build(set);
    }

    /** Questions to pick from when swapping by hand: matching the search text, not already in the quiz, same kind as the one replaced. */
    @Transactional(readOnly = true)
    public List<DailyQuizPreviewDto.Candidate> candidates(Long forQuestionId, String term) {
        DailyQuizSet set = requireSet();
        Question old = questionRepository.findById(forQuestionId).orElseThrow(() -> notFound(forQuestionId));
        String needle = term == null ? "" : term.trim().toLowerCase();
        List<DailyQuizExclusion> exclusions = exclusionRepository.findByQuizDate(set.getQuizDate());
        Map<Long, LocalDate> lastUsed = quizService.lastUsedBefore(set.getQuizDate(), set.getQuizDate());
        String slot = slotOf(old);
        return questionRepository.findByLanguageWithLabels(Language.NO).stream()
                .filter(q -> !set.getQuestionIds().contains(q.getId()))
                .filter(q -> slotOf(q).equals(slot))
                .filter(q -> slot.equals("LOGO") ? DailyQuizService.isLogoQuestionWithPhoto(q) : true)
                .filter(q -> slot.equals("YEAR") ? DailyQuizService.isYearQuestionWithYearAnswer(q) : true)
                .filter(q -> exclusions.stream().noneMatch(e -> e.matches(q)))
                .filter(q -> needle.isEmpty()
                        || q.getQuestionText().toLowerCase().contains(needle)
                        || q.getAnswer().toLowerCase().contains(needle)
                        || (q.getCategory() != null && q.getCategory().toLowerCase().contains(needle)))
                .sorted(Comparator.comparing((Question q) -> lastUsed.containsKey(q.getId())).thenComparing(Question::getId))
                .limit(CANDIDATE_LIMIT)
                .map(q -> new DailyQuizPreviewDto.Candidate(q.getId(), q.getQuestionText(), q.getAnswer(), q.getCategory(),
                        labelNames(q), lastUsed.get(q.getId())))
                .collect(Collectors.toList());
    }

    // ---- internals ----

    private DailyQuizSet requireSet() {
        LocalDate date = previewDate();
        return setRepository.findByQuizDate(date).orElseGet(() -> quizService.generateSet(date));
    }

    private static String slotOf(Question q) {
        if (DailyQuizService.isLogoCategory(q)) return "LOGO";
        if (DailyQuizScoring.isYearQuestion(q)) return "YEAR";
        return "NORMAL";
    }

    private int indexOf(DailyQuizSet set, Long questionId) {
        int index = set.getQuestionIds().indexOf(questionId);
        if (index < 0) throw new IllegalArgumentException("Det spørsmålet er ikke med i quizen.");
        return index;
    }

    /**
     * A question of the same kind as `old`, outside the quiz and not ruled out. Prefers ones that haven't been
     * in a daily quiz lately (random among them); if every one has, the one used longest ago.
     */
    private Optional<Question> pickReplacement(DailyQuizSet set, Question old) {
        LocalDate date = set.getQuizDate();
        List<DailyQuizExclusion> exclusions = exclusionRepository.findByQuizDate(date);
        String slot = slotOf(old);
        Predicate<Question> fits = switch (slot) {
            case "LOGO" -> DailyQuizService::isLogoQuestionWithPhoto;
            case "YEAR" -> DailyQuizService::isYearQuestionWithYearAnswer;
            default -> q -> !DailyQuizService.isLogoCategory(q) && !DailyQuizScoring.isYearQuestion(q);
        };
        List<Question> pool = questionRepository.findByLanguageWithLabels(Language.NO).stream()
                .filter(q -> !set.getQuestionIds().contains(q.getId()))
                .filter(fits)
                .filter(q -> exclusions.stream().noneMatch(e -> e.matches(q)))
                .collect(Collectors.toList());
        if (pool.isEmpty()) return Optional.empty();
        Map<Long, LocalDate> lastUsed = quizService.lastUsedBefore(date, date);
        List<Question> fresh = pool.stream().filter(q -> !lastUsed.containsKey(q.getId())).collect(Collectors.toList());
        if (!fresh.isEmpty()) return Optional.of(fresh.get(new Random().nextInt(fresh.size())));
        return pool.stream().min(Comparator.comparing((Question q) -> lastUsed.get(q.getId())));
    }

    /** Puts `question` in slot `index` of the quiz and keeps the "used on this day" log in step. */
    private void put(DailyQuizSet set, int index, Question question) {
        Long oldId = set.getQuestionIds().get(index);
        List<Long> ids = new ArrayList<>(set.getQuestionIds());
        ids.set(index, question.getId());
        set.setQuestionIds(ids);
        setRepository.save(set);
        LocalDate date = set.getQuizDate();
        useRepository.deleteAll(useRepository.findByQuizDate(date).stream()
                .filter(u -> u.getQuestionId().equals(oldId)).collect(Collectors.toList()));
        useRepository.save(new DailyQuizQuestionUse(question.getId(), date));
    }

    private static List<String> labelNames(Question q) {
        return q.getLabels().stream().map(QuestionLabel::getName).sorted(String.CASE_INSENSITIVE_ORDER).collect(Collectors.toList());
    }

    private static String key(String category) {
        return category == null ? "" : category.trim().toLowerCase();
    }

    private DailyQuizPreviewDto build(DailyQuizSet set) {
        LocalDate date = set.getQuizDate();
        List<DailyQuizExclusion> exclusions = exclusionRepository.findByQuizDate(date);
        Map<Long, Question> byId = questionRepository.findAllById(set.getQuestionIds()).stream()
                .collect(Collectors.toMap(Question::getId, q -> q));
        Map<Long, LocalDate> lastUsed = quizService.lastUsedBefore(date, date);

        List<DailyQuizPreviewDto.Item> items = new ArrayList<>();
        int position = 1;
        for (Long id : set.getQuestionIds()) {
            Question q = byId.get(id);
            if (q == null) { position++; continue; } // deleted from the bank since - nothing to show
            items.add(new DailyQuizPreviewDto.Item(position++, q.getId(), q.getQuestionText(), q.getAnswer(),
                    q.getCategory(), labelNames(q), q.getDifficultyLevel(), q.getPhotoUrl(), slotOf(q),
                    exclusions.stream().anyMatch(e -> e.matches(q)), lastUsed.get(q.getId())));
        }

        // Category counts for this quiz and the days before it (whichever of those quizzes still exist).
        List<LocalDate> days = new ArrayList<>();
        List<Map<String, Integer>> countsPerDay = new ArrayList<>();
        Map<String, String> display = new LinkedHashMap<>();
        for (int i = 0; i < HISTORY_DAYS; i++) {
            LocalDate day = date.minusDays(i);
            days.add(day);
            Map<String, Integer> counts = new HashMap<>();
            Optional<DailyQuizSet> daySet = i == 0 ? Optional.of(set) : setRepository.findByQuizDate(day);
            if (daySet.isPresent()) {
                Map<Long, Question> dayQuestions = i == 0 ? byId : questionRepository.findAllById(daySet.get().getQuestionIds()).stream()
                        .collect(Collectors.toMap(Question::getId, q -> q));
                for (Long id : daySet.get().getQuestionIds()) {
                    Question q = dayQuestions.get(id);
                    if (q == null || q.getCategory() == null) continue;
                    counts.merge(key(q.getCategory()), 1, Integer::sum);
                    display.putIfAbsent(key(q.getCategory()), q.getCategory().trim());
                }
            }
            countsPerDay.add(counts);
        }
        List<DailyQuizPreviewDto.CategoryRow> history = new ArrayList<>();
        for (Map.Entry<String, String> e : display.entrySet()) {
            if (countsPerDay.get(0).getOrDefault(e.getKey(), 0) == 0) continue; // only categories in the previewed quiz
            history.add(new DailyQuizPreviewDto.CategoryRow(e.getValue(),
                    countsPerDay.stream().map(m -> m.getOrDefault(e.getKey(), 0)).collect(Collectors.toList())));
        }
        history.sort(Comparator.comparing((DailyQuizPreviewDto.CategoryRow r) -> r.counts().stream().mapToInt(Integer::intValue).sum()).reversed()
                .thenComparing(DailyQuizPreviewDto.CategoryRow::category, String.CASE_INSENSITIVE_ORDER));

        Map<String, String> categories = new TreeMap<>();
        for (QuestionRepository.CategoryCount row : questionRepository.countByCategoryForLanguage(Language.NO)) {
            if (row.getCategory() != null && !row.getCategory().isBlank()) categories.putIfAbsent(key(row.getCategory()), row.getCategory().trim());
        }
        List<String> labels = labelRepository.findAll().stream().map(QuestionLabel::getName)
                .sorted(String.CASE_INSENSITIVE_ORDER).collect(Collectors.toList());

        return new DailyQuizPreviewDto(date, set.getId(), items,
                exclusions.stream().map(e -> new DailyQuizPreviewDto.Exclusion(e.getKind(), e.getValue())).collect(Collectors.toList()),
                history, days, new ArrayList<>(categories.values()), labels);
    }

    private static ResourceNotFoundException notFound(Long id) {
        return new ResourceNotFoundException("Fant ikke spørsmål med id " + id);
    }
}
