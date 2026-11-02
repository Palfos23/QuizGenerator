package com.quizapp.service;

import com.quizapp.dto.AdventDto.*;
import com.quizapp.exception.ResourceNotFoundException;
import com.quizapp.model.AdventAlias;
import com.quizapp.model.AdventDay;
import com.quizapp.model.AdventEntry;
import com.quizapp.model.AdventType;
import com.quizapp.repository.AdventAliasRepository;
import com.quizapp.repository.AdventDayRepository;
import com.quizapp.repository.AdventEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * The advent calendar, game master side: fill in the 24 doors ahead of time, and - once players have answered - go
 * through what they wrote. Players' answers are matched automatically (same text after ignoring case, accents and
 * punctuation); the game master fixes the rest by deciding how a spelling should be read, which is stored once for the
 * normalised text and so applies to everyone who wrote it. Points are recalculated from that every time, nothing to redo.
 */
@Service
public class AdventAdminService {

    private final AdventDayRepository dayRepository;
    private final AdventEntryRepository entryRepository;
    private final AdventAliasRepository aliasRepository;
    private final AdventClock clock;

    public AdventAdminService(AdventDayRepository dayRepository, AdventEntryRepository entryRepository,
                              AdventAliasRepository aliasRepository, AdventClock clock) {
        this.clock = clock;
        this.dayRepository = dayRepository;
        this.entryRepository = entryRepository;
        this.aliasRepository = aliasRepository;
    }

    private static void checkDay(int day) {
        if (day < 1 || day > AdventService.DOORS) throw new ResourceNotFoundException("Fant ikke luke " + day + ".");
    }

    // ------------------------------------------------------------------ the 24 doors

    @Transactional(readOnly = true)
    public AdminSeasonDto season() {
        return season(clock.today());
    }

    @Transactional(readOnly = true)
    AdminSeasonDto season(LocalDate today) {
        int year = AdventService.seasonYear(today);
        Map<Integer, AdventDay> days = dayRepository.findBySeasonYearOrderByDayNumberAsc(year).stream()
                .collect(Collectors.toMap(AdventDay::getDayNumber, d -> d));
        Map<Long, Long> entryCounts = days.isEmpty() ? Map.of() : entryRepository
                .findByDayIdIn(days.values().stream().map(AdventDay::getId).collect(Collectors.toList())).stream()
                .collect(Collectors.groupingBy(AdventEntry::getDayId, Collectors.counting()));
        List<AdminDayRowDto> rows = new ArrayList<>();
        for (int n = 1; n <= AdventService.DOORS; n++) {
            LocalDate date = AdventService.doorDate(year, n);
            AdventDay d = days.get(n);
            rows.add(new AdminDayRowDto(n, date, d != null && d.isReady(), d == null ? null : d.getCategory(),
                    d == null ? null : d.getType().name(), d == null ? 0 : entryCounts.getOrDefault(d.getId(), 0L),
                    d != null && d.isApproved(), date.isBefore(today)));
        }
        return new AdminSeasonDto(year, today, rows);
    }

    @Transactional(readOnly = true)
    public AdminDayDto day(int day) {
        return day(day, clock.today());
    }

    @Transactional(readOnly = true)
    AdminDayDto day(int n, LocalDate today) {
        checkDay(n);
        int year = AdventService.seasonYear(today);
        LocalDate date = AdventService.doorDate(year, n);
        Optional<AdventDay> found = dayRepository.findBySeasonYearAndDayNumber(year, n);
        if (found.isEmpty()) {
            return new AdminDayDto(n, date, null, AdventType.NORMAL.name(), List.of(), null, 0, false, date.isBefore(today), false);
        }
        AdventDay d = found.get();
        long entries = entryRepository.countByDayId(d.getId());
        return new AdminDayDto(n, date, d.getCategory(), d.getType().name(), d.getAnswers(), d.getWildcardIndex(), entries,
                d.isApproved(), date.isBefore(today), entries > 0);
    }

    @Transactional
    public AdminDayDto saveDay(int day, AdminDayRequest request) {
        return saveDay(day, request, clock.today());
    }

    @Transactional
    AdminDayDto saveDay(int n, AdminDayRequest request, LocalDate today) {
        checkDay(n);
        int year = AdventService.seasonYear(today);
        AdventType type;
        try {
            type = AdventType.valueOf(request.type() == null ? "NORMAL" : request.type());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Ukjent type luke.");
        }
        String category = request.category() == null ? "" : request.category().trim();
        if (category.length() > 200) throw new IllegalArgumentException("Kategorien kan ikke være lengre enn 200 tegn.");

        List<String> answers = (request.answers() == null ? List.<String>of() : request.answers()).stream()
                .map(a -> a == null ? "" : a.replaceAll("\\s+", " ").trim())
                .filter(a -> !a.isEmpty()).collect(Collectors.toList());
        if (answers.size() > type.answersRequired()) {
            throw new IllegalArgumentException(type == AdventType.ONE_OF_FOUR
                    ? "«Én av fire»-luka har bare ett svar."
                    : "En luke har 4 svar.");
        }
        if (answers.stream().anyMatch(a -> a.length() > AdventService.MAX_ANSWER_LENGTH)) {
            throw new IllegalArgumentException("Svarene kan ikke være lengre enn " + AdventService.MAX_ANSWER_LENGTH + " tegn.");
        }
        if (answers.stream().map(AdventScoring::normalize).distinct().count() != answers.size()) {
            throw new IllegalArgumentException("Du har skrevet det samme svaret to ganger.");
        }
        Integer wildcard = type == AdventType.WILDCARD ? request.wildcardIndex() : null;
        if (wildcard != null && (wildcard < 0 || wildcard >= answers.size())) {
            throw new IllegalArgumentException("Wildcard må være ett av svarene.");
        }

        AdventDay d = dayRepository.findBySeasonYearAndDayNumber(year, n).orElseGet(() -> {
            AdventDay fresh = new AdventDay();
            fresh.setSeasonYear(year);
            fresh.setDayNumber(n);
            return fresh;
        });
        boolean hasEntries = d.getId() != null && entryRepository.countByDayId(d.getId()) > 0;
        boolean rulesChanged = d.getType() != type || !d.getAnswers().equals(answers) || !Objects.equals(d.getWildcardIndex(), wildcard);
        if (hasEntries && rulesChanged) {
            throw new IllegalStateException("Spillere har allerede svart på denne luka - type og svar kan ikke endres. "
                    + "Bruk gjennomgangen for å rette hvordan et svar leses.");
        }
        d.setCategory(category);
        d.setType(type);
        d.setAnswers(answers);
        d.setWildcardIndex(wildcard);
        dayRepository.save(d);
        return day(n, today);
    }

    // ------------------------------------------------------------------ going through the answers

    @Transactional(readOnly = true)
    public ReviewDto review(int day) {
        return review(day, clock.today());
    }

    @Transactional(readOnly = true)
    ReviewDto review(int n, LocalDate today) {
        checkDay(n);
        int year = AdventService.seasonYear(today);
        AdventDay d = dayRepository.findBySeasonYearAndDayNumber(year, n)
                .orElseThrow(() -> new ResourceNotFoundException("Denne luka er ikke fylt ut ennå."));
        boolean closed = AdventService.doorDate(year, n).isBefore(today);
        List<AdventEntry> entries = entryRepository.findByDayId(d.getId());
        Map<String, Integer> corrections = corrections(d.getId());
        List<String> hidden = d.getAnswers();

        Map<String, Map<String, Integer>> spellings = new HashMap<>();
        Map<String, Integer> counts = new HashMap<>();
        for (AdventEntry e : entries) {
            Set<String> seen = new HashSet<>();
            for (String raw : e.getAnswers()) {
                String norm = AdventScoring.normalize(raw);
                if (norm.isEmpty() || !seen.add(norm)) continue;
                counts.merge(norm, 1, Integer::sum);
                spellings.computeIfAbsent(norm, k -> new HashMap<>()).merge(raw.trim(), 1, Integer::sum);
            }
        }
        List<ReviewAnswerDto> items = counts.entrySet().stream()
                .map(c -> {
                    String shown = spellings.get(c.getKey()).entrySet().stream()
                            .max(Map.Entry.<String, Integer>comparingByValue().thenComparing(Map.Entry.comparingByKey()))
                            .orElseThrow().getKey();
                    return new ReviewAnswerDto(shown, c.getKey(), c.getValue(),
                            AdventScoring.matchIndex(shown, hidden, corrections), corrections.containsKey(c.getKey()));
                })
                .sorted(Comparator.comparingInt(ReviewAnswerDto::count).reversed().thenComparing(ReviewAnswerDto::normalized))
                .collect(Collectors.toList());
        int gm = d.isReady() ? AdventScoring.DayResult.compute(d, entries, corrections).gamemasterPoints : 0;
        return new ReviewDto(n, d.getCategory(), d.getType().name(), hidden, entries.size(), d.isApproved(), closed, gm, items);
    }

    /** Read this text as hidden answer answerIndex, as no match (-1), or - null - the automatic way again. */
    @Transactional
    public ReviewDto decide(int day, DecisionRequest request) {
        return decide(day, request, clock.today());
    }

    @Transactional
    ReviewDto decide(int n, DecisionRequest request, LocalDate today) {
        checkDay(n);
        int year = AdventService.seasonYear(today);
        AdventDay d = dayRepository.findBySeasonYearAndDayNumber(year, n)
                .orElseThrow(() -> new ResourceNotFoundException("Denne luka er ikke fylt ut ennå."));
        String norm = AdventScoring.normalize(request.text());
        if (norm.isEmpty()) throw new IllegalArgumentException("Mangler svaret.");
        Optional<AdventAlias> existing = aliasRepository.findByDayIdAndNormalizedText(d.getId(), norm);
        if (request.answerIndex() == null) {
            existing.ifPresent(aliasRepository::delete);
        } else {
            int idx = request.answerIndex();
            if (idx < -1 || idx >= d.getAnswers().size()) throw new IllegalArgumentException("Ugyldig svar å knytte til.");
            AdventAlias alias = existing.orElseGet(AdventAlias::new);
            alias.setDayId(d.getId());
            alias.setNormalizedText(norm);
            alias.setAnswerIndex(idx);
            aliasRepository.save(alias);
        }
        return review(n, today);
    }

    /** Marks the door as gone through - players stop seeing its results as preliminary. */
    @Transactional
    public ReviewDto approve(int day, boolean approved) {
        return approve(day, approved, clock.today());
    }

    @Transactional
    ReviewDto approve(int n, boolean approved, LocalDate today) {
        checkDay(n);
        AdventDay d = dayRepository.findBySeasonYearAndDayNumber(AdventService.seasonYear(today), n)
                .orElseThrow(() -> new ResourceNotFoundException("Denne luka er ikke fylt ut ennå."));
        d.setApproved(approved);
        dayRepository.save(d);
        return review(n, today);
    }

    // ------------------------------------------------------------------ test mode

    /** Whether a pretend date is on, and whether the test data may be wiped (only while the real season hasn't started). */
    @Transactional(readOnly = true)
    public TestModeDto testMode() {
        return testMode(clock.realToday());
    }

    TestModeDto testMode(LocalDate realToday) {
        LocalDate date = clock.simulatedDate();
        return new TestModeDto(date != null, date, clock.simulatedUntil(), realToday, canReset(realToday));
    }

    private static boolean canReset(LocalDate realToday) {
        return realToday.isBefore(AdventService.doorDate(AdventService.seasonYear(realToday), 1));
    }

    /** Pretend it is this date for the next few hours (null = back to the real date). Everyone sees the calendar on it. */
    public TestModeDto setTestDate(LocalDate date) {
        return setTestDate(date, clock.realToday());
    }

    TestModeDto setTestDate(LocalDate date, LocalDate realToday) {
        if (date == null) {
            clock.clear();
        } else {
            int year = AdventService.seasonYear(realToday);
            if (date.isBefore(LocalDate.of(year, 11, 1)) || date.isAfter(LocalDate.of(year + 1, 1, 31))) {
                throw new IllegalArgumentException("Velg en dato mellom 1. november og 31. januar.");
            }
            clock.simulate(date);
        }
        return testMode(realToday);
    }

    /**
     * Wipes everything players did on this season's doors (entries, the game master's corrections, "gjennomgått" marks) so
     * a test run leaves nothing behind. The doors themselves stay. Only allowed before the season really starts - never
     * once real players have answered.
     */
    @Transactional
    public TestModeDto resetTestData() {
        return resetTestData(clock.realToday());
    }

    @Transactional
    TestModeDto resetTestData(LocalDate realToday) {
        if (!canReset(realToday)) throw new IllegalStateException("Testdata kan bare nullstilles før julekalenderen har startet.");
        List<AdventDay> days = dayRepository.findBySeasonYearOrderByDayNumberAsc(AdventService.seasonYear(realToday));
        List<Long> ids = days.stream().map(AdventDay::getId).collect(Collectors.toList());
        if (!ids.isEmpty()) {
            entryRepository.deleteByDayIdIn(ids);
            aliasRepository.deleteByDayIdIn(ids);
            days.forEach(d -> d.setApproved(false));
            dayRepository.saveAll(days);
        }
        return testMode(realToday);
    }

    private Map<String, Integer> corrections(Long dayId) {
        Map<String, Integer> out = new HashMap<>();
        aliasRepository.findByDayId(dayId).forEach(a -> out.put(a.getNormalizedText(), a.getAnswerIndex()));
        return out;
    }
}
