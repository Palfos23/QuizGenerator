package com.quizapp.service;

import com.quizapp.dto.TensionAnswerEntryDto;
import com.quizapp.dto.TensionQuestionDto;
import com.quizapp.exception.ResourceNotFoundException;
import com.quizapp.model.TensionAnswerEntry;
import com.quizapp.model.TensionQuestion;
import com.quizapp.repository.AthleteRepository;
import com.quizapp.repository.TensionQuestionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class TensionQuestionService {

    private static final Logger log = LoggerFactory.getLogger(TensionQuestionService.class);

    private final TensionQuestionRepository questionRepository;
    private final AthleteRepository athleteRepository;

    public TensionQuestionService(TensionQuestionRepository questionRepository, AthleteRepository athleteRepository) {
        this.questionRepository = questionRepository;
        this.athleteRepository = athleteRepository;
    }

    @Transactional(readOnly = true)
    public List<com.quizapp.dto.TensionQuestionSummaryDto> findAll() {
        return questionRepository.findAllSummaries().stream()
                .map(p -> new com.quizapp.dto.TensionQuestionSummaryDto(
                        p.getId(), p.getTitle(), p.getMainCategory(), p.getAnswersCategory(),
                        p.getSource(), p.getSafeCount(), p.getTensionCount(), Boolean.TRUE.equals(p.getCanExpire()),
                        p.getUpdatedAt()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TensionQuestionDto getOne(Long id) {
        TensionQuestion q = questionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No tension question found with id " + id));
        return toDto(q);
    }

    @Transactional(readOnly = true)
    public List<TensionQuestionDto> getRandom(int count, String mainCategory, List<String> excludeCategories) {
        return loadRandomSubset(candidateIds(mainCategory, excludeCategories), count);
    }

    // For the "pick your question" round-start screen: a small pool of
    // candidates for this specific round, excluding whatever's already been
    // played this game so a repeat never gets offered.
    @Transactional(readOnly = true)
    public List<TensionQuestionDto> getRoundChoices(int count, String mainCategory, List<String> excludeCategories, List<Long> excludeIds) {
        List<Long> ids = candidateIds(mainCategory, excludeCategories);
        List<Long> available = ids.stream()
                .filter(id -> excludeIds == null || !excludeIds.contains(id))
                .collect(Collectors.toList());
        return loadRandomSubset(available, count);
    }

    // A specific category (if set) takes priority - excluding categories only
    // makes sense for the "draw from everything" case, not "just this one".
    private List<Long> candidateIds(String mainCategory, List<String> excludeCategories) {
        if (mainCategory != null && !mainCategory.isBlank()) {
            return questionRepository.findIdsByMainCategoryIgnoreCase(mainCategory);
        }
        if (excludeCategories != null && !excludeCategories.isEmpty()) {
            List<String> excludedLower = excludeCategories.stream()
                    .map(String::toLowerCase)
                    .collect(Collectors.toList());
            return questionRepository.findIdsByMainCategoryNotIn(excludedLower);
        }
        return questionRepository.findAllIds();
    }

    // Shuffling and sampling at the ID level is cheap regardless of how many
    // questions exist in the category - only the small sampled subset actually
    // gets its full entity (and answer lists) loaded.
    private List<TensionQuestionDto> loadRandomSubset(List<Long> ids, int count) {
        List<Long> shuffled = new ArrayList<>(ids);
        Collections.shuffle(shuffled);
        List<Long> sampled = shuffled.stream().limit(count).collect(Collectors.toList());
        return questionRepository.findAllById(sampled).stream()
                .map(TensionQuestionService::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<String> getDistinctMainCategories() {
        return questionRepository.findDistinctMainCategories();
    }

    // Powers the answer-box autocomplete for a question whose answersFromSubjects
    // is true - the player-facing equivalent of TensionCategoryService.getOptions,
    // just sourced from Subjects (athletes) in a sport instead of a hand-curated
    // TensionCategory word list.
    // Cached for a short while per sport: every player in a room asks for this same list the
    // instant a round starts, and it's reference data that barely ever changes - so a room of
    // N players is one database read instead of N. Short enough that a newly added athlete
    // shows up within a couple of minutes.
    private static final long SUBJECT_OPTIONS_TTL_MS = 2 * 60 * 1000;
    private record CachedOptions(List<String> names, long loadedAt) { }
    private final Map<String, CachedOptions> subjectOptionsCache = new ConcurrentHashMap<>();

    @Transactional(readOnly = true)
    public List<String> getSubjectOptions(String sport) {
        String requested = sport == null ? "" : sport.trim();
        String cacheKey = requested.toLowerCase();
        CachedOptions cached = subjectOptionsCache.get(cacheKey);
        if (cached != null && System.currentTimeMillis() - cached.loadedAt() < SUBJECT_OPTIONS_TTL_MS) {
            return cached.names();
        }

        long startedAt = System.currentTimeMillis();
        List<String> names = athleteRepository.findNamesBySport(requested);
        boolean usedLooseMatch = false;
        if (names.isEmpty()) {
            names = athleteRepository.findNamesBySportLoose(requested);
            usedLooseMatch = !names.isEmpty();
        }
        long tookMs = System.currentTimeMillis() - startedAt;

        if (names.isEmpty()) {
            // The answer box would be silently suggestion-less - name what WAS asked for and
            // what actually exists, which is almost always enough to spot a mismatch.
            log.warn("Tension subject-options: NO athletes found for sport '{}' (took {} ms). Known sports: {}",
                    requested, tookMs, athleteRepository.findDistinctSports());
            // Deliberately not cached: an empty result should be re-checked on the next request.
            return names;
        }
        if (usedLooseMatch) {
            log.warn("Tension subject-options: sport '{}' only matched ignoring case/whitespace - a question or "
                    + "category is storing a slightly different name than the athletes use. Served {} names anyway.",
                    requested, names.size());
        } else {
            log.info("Tension subject-options: sport '{}' -> {} names in {} ms", requested, names.size(), tookMs);
        }
        subjectOptionsCache.put(cacheKey, new CachedOptions(names, System.currentTimeMillis()));
        return names;
    }

    @Transactional
    public TensionQuestionDto create(TensionQuestionDto dto) {
        TensionQuestion q = new TensionQuestion();
        applyDto(q, dto);
        return toDto(questionRepository.save(q));
    }

    @Transactional
    public TensionQuestionDto update(Long id, TensionQuestionDto dto) {
        TensionQuestion q = questionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No tension question found with id " + id));
        applyDto(q, dto);
        return toDto(questionRepository.save(q));
    }

    @Transactional
    public void delete(Long id) {
        if (!questionRepository.existsById(id)) {
            throw new ResourceNotFoundException("No tension question found with id " + id);
        }
        questionRepository.deleteById(id);
    }

    private void applyDto(TensionQuestion q, TensionQuestionDto dto) {
        q.setTitle(dto.getTitle());
        q.setMainCategory(dto.getMainCategory());
        q.setAnswersCategory(dto.getAnswersCategory());
        q.setAnswersFromSubjects(dto.isAnswersFromSubjects());
        q.setAnswersSport(dto.getAnswersSport());
        q.setSource(dto.getSource());
        q.setTiebreaker(dto.getTiebreaker());
        q.setCanExpire(dto.isCanExpire());
        q.setSafeAnswers(toEntryEntities(dto.getSafeAnswers()));
        q.setTensionAnswers(toEntryEntities(dto.getTensionAnswers()));
        q.setUpdatedAt(java.time.Instant.now());
    }

    private List<TensionAnswerEntry> toEntryEntities(List<TensionAnswerEntryDto> dtos) {
        if (dtos == null) return new ArrayList<>();
        return dtos.stream().map(d -> {
            TensionAnswerEntry e = new TensionAnswerEntry();
            e.setRank(d.getRank());
            e.setText(d.getText());
            return e;
        }).collect(Collectors.toList());
    }

    static TensionQuestionDto toDto(TensionQuestion q) {
        TensionQuestionDto dto = new TensionQuestionDto();
        dto.setId(q.getId());
        dto.setTitle(q.getTitle());
        dto.setUpdatedAt(q.getUpdatedAt());
        dto.setMainCategory(q.getMainCategory());
        dto.setAnswersCategory(q.getAnswersCategory());
        dto.setAnswersFromSubjects(q.isAnswersFromSubjects());
        dto.setAnswersSport(q.getAnswersSport());
        dto.setSource(q.getSource());
        dto.setTiebreaker(q.getTiebreaker());
        dto.setCanExpire(q.isCanExpire());
        dto.setSafeAnswers(q.getSafeAnswers().stream()
                .sorted((a, b) -> a.getRank() - b.getRank())
                .map(TensionQuestionService::toEntryDto)
                .collect(Collectors.toList()));
        dto.setTensionAnswers(q.getTensionAnswers().stream()
                .sorted((a, b) -> a.getRank() - b.getRank())
                .map(TensionQuestionService::toEntryDto)
                .collect(Collectors.toList()));
        return dto;
    }

    private static TensionAnswerEntryDto toEntryDto(TensionAnswerEntry e) {
        TensionAnswerEntryDto dto = new TensionAnswerEntryDto();
        dto.setId(e.getId());
        dto.setRank(e.getRank());
        dto.setText(e.getText());
        return dto;
    }
}
