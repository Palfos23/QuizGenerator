package com.quizapp.service;

import com.quizapp.dto.AdventDto.*;
import com.quizapp.exception.ResourceNotFoundException;
import com.quizapp.model.*;
import com.quizapp.repository.*;
import com.quizapp.service.AdventScoring.DayResult;
import com.quizapp.service.AdventScoring.EntryResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * The advent calendar, player side. Door N of a season opens on N December (Norwegian time) and closes at midnight; the
 * next morning its results are out. A player's points are worked out here from their entries and the game master's
 * answers every time they are asked for, never stored - so when the game master corrects how an answer is read
 * (AdventAdminService), everybody's points follow.
 *
 * Each public method has a package-private twin that takes "today", so the date logic can be tested without waiting for
 * December.
 */
@Service
public class AdventService {

    public static final ZoneId OSLO = ZoneId.of("Europe/Oslo");
    static final String BEFORE = "BEFORE";
    static final String RUNNING = "RUNNING";
    static final String FINISHED = "FINISHED";
    static final int DOORS = 24;
    static final int MAX_ANSWER_LENGTH = 100;
    static final int LEADERBOARD_SIZE = 50;
    static final int CROWD_SIZE = 8;

    private final AdventDayRepository dayRepository;
    private final AdventEntryRepository entryRepository;
    private final AdventAliasRepository aliasRepository;
    private final AdventProfileRepository profileRepository;
    private final AppUserRepository appUserRepository;
    private final AdventClock clock;

    public AdventService(AdventDayRepository dayRepository, AdventEntryRepository entryRepository,
                         AdventAliasRepository aliasRepository, AdventProfileRepository profileRepository,
                         AppUserRepository appUserRepository, AdventClock clock) {
        this.dayRepository = dayRepository;
        this.entryRepository = entryRepository;
        this.aliasRepository = aliasRepository;
        this.profileRepository = profileRepository;
        this.appUserRepository = appUserRepository;
        this.clock = clock;
    }

    // ------------------------------------------------------------------ the calendar

    /** January still belongs to the season that just ended (so its final standings and "wrapped" stay up). */
    static int seasonYear(LocalDate today) {
        return today.getMonth() == Month.JANUARY ? today.getYear() - 1 : today.getYear();
    }

    static LocalDate doorDate(int year, int day) {
        return LocalDate.of(year, Month.DECEMBER, day);
    }

    static String phase(int year, LocalDate today) {
        if (today.isBefore(doorDate(year, 1))) return BEFORE;
        if (today.isAfter(doorDate(year, DOORS))) return FINISHED;
        return RUNNING;
    }

    /** Everything stored about one season, with the closed doors already scored. */
    static final class Season {
        final int year;
        final LocalDate today;
        final String phase;
        final Map<Integer, AdventDay> days = new TreeMap<>();
        final Map<Long, List<AdventEntry>> entriesByDay = new HashMap<>();
        final Map<Long, Map<String, Integer>> corrections = new HashMap<>();
        /** The doors that are over and were filled in, by number. */
        final Map<Integer, DayResult> results = new TreeMap<>();

        Season(int year, LocalDate today) {
            this.year = year;
            this.today = today;
            this.phase = phase(year, today);
        }

        LocalDate date(int day) {
            return doorDate(year, day);
        }

        boolean ready(int day) {
            AdventDay d = days.get(day);
            return d != null && d.isReady();
        }

        /** LOCKED (opens later) | OPEN (today) | CLOSED (over) | EMPTY (nothing was put in this door). */
        String status(int day) {
            LocalDate date = date(day);
            if (date.isAfter(today)) return "LOCKED";
            if (date.equals(today)) return ready(day) ? "OPEN" : "EMPTY";
            return ready(day) ? "CLOSED" : "EMPTY";
        }
    }

    Season load(int year, LocalDate today) {
        Season s = new Season(year, today);
        List<AdventDay> days = dayRepository.findBySeasonYearOrderByDayNumberAsc(year);
        days.forEach(d -> s.days.put(d.getDayNumber(), d));
        List<Long> ids = days.stream().map(AdventDay::getId).collect(Collectors.toList());
        if (!ids.isEmpty()) {
            entryRepository.findByDayIdIn(ids).forEach(e -> s.entriesByDay.computeIfAbsent(e.getDayId(), k -> new ArrayList<>()).add(e));
            for (AdventAlias a : aliasRepository.findByDayIdIn(ids)) {
                s.corrections.computeIfAbsent(a.getDayId(), k -> new HashMap<>()).put(a.getNormalizedText(), a.getAnswerIndex());
            }
        }
        for (AdventDay d : days) {
            if ("CLOSED".equals(s.status(d.getDayNumber()))) {
                s.results.put(d.getDayNumber(), DayResult.compute(d, s.entriesByDay.getOrDefault(d.getId(), List.of()),
                        s.corrections.getOrDefault(d.getId(), Map.of())));
            }
        }
        return s;
    }

    private Optional<AppUser> findPlayer(String email) {
        return appUserRepository.findByEmail(email);
    }

    // ------------------------------------------------------------------ the calendar page

    @Transactional(readOnly = true)
    public StateDto state(String email) {
        return state(email, clock.today());
    }

    @Transactional(readOnly = true)
    StateDto state(String email, LocalDate today) {
        int year = seasonYear(today);
        Season s = load(year, today);
        LocalDate start = doorDate(year, 1);
        Optional<AppUser> me = findPlayer(email);
        Long myId = me.map(AppUser::getId).orElse(null);
        boolean wrapped = FINISHED.equals(s.phase);

        if (BEFORE.equals(s.phase)) {
            return new StateDto(s.phase, year, start, (int) ChronoUnit.DAYS.between(today, start), null, me.isPresent(), List.of(),
                    AdventScoring.JOKERS_PER_SEASON, AdventScoring.JOKERS_PER_SEASON, 0, null, 0, 0,
                    showName(myId), false, clock.simulatedDate());
        }

        MyStats mine = myStats(s, myId);
        List<DoorDto> doors = new ArrayList<>();
        for (int n = 1; n <= DOORS; n++) {
            String status = s.status(n);
            AdventDay day = s.days.get(n);
            boolean shown = "OPEN".equals(status) || "CLOSED".equals(status);
            EntryResult r = mine.results.get(n);
            doors.add(new DoorDto(n, s.date(n), status, mine.entered.contains(n), shown && day != null ? day.getType().name() : null,
                    r == null ? null : r.points(), r == null ? null : r.matches()));
        }
        Integer rank = null;
        for (StandingDto row : standings(s, myId)) {
            if (row.you()) rank = row.rank();
        }
        Integer todayDay = RUNNING.equals(s.phase) ? today.getDayOfMonth() : null;
        return new StateDto(s.phase, year, start, 0, todayDay, me.isPresent(), doors,
                Math.max(0, AdventScoring.JOKERS_PER_SEASON - mine.jokersUsed), AdventScoring.JOKERS_PER_SEASON, mine.total, rank,
                mine.streak, mine.bestStreak, showName(myId), wrapped, clock.simulatedDate());
    }

    private boolean showName(Long userId) {
        return userId == null || profileRepository.findById(userId).map(AdventProfile::isShowName).orElse(true);
    }

    @Transactional
    public void setShowName(String email, boolean showName) {
        AppUser user = findPlayer(email).orElseThrow(() -> new IllegalStateException("Bare spillere kan velge dette."));
        AdventProfile profile = profileRepository.findById(user.getId()).orElseGet(() -> new AdventProfile(user.getId(), true));
        profile.setShowName(showName);
        profileRepository.save(profile);
    }

    // ------------------------------------------------------------------ one player's numbers

    /** Everything about one player across the season: their closed-door results, streaks and jokers. */
    static final class MyStats {
        final Map<Integer, EntryResult> results = new TreeMap<>();
        final Set<Integer> entered = new TreeSet<>();
        int total;
        int jokersUsed;
        int streak;
        int bestStreak;
    }

    MyStats myStats(Season s, Long userId) {
        MyStats m = new MyStats();
        if (userId == null) return m;
        for (AdventDay d : s.days.values()) {
            for (AdventEntry e : s.entriesByDay.getOrDefault(d.getId(), List.of())) {
                if (!e.getUserId().equals(userId)) continue;
                m.entered.add(d.getDayNumber());
                if (e.isJokerUsed()) m.jokersUsed++;
            }
        }
        for (Map.Entry<Integer, DayResult> r : s.results.entrySet()) {
            EntryResult er = r.getValue().byUser.get(userId);
            if (er != null) {
                m.results.put(r.getKey(), er);
                m.total += er.points();
            }
        }

        int upTo = FINISHED.equals(s.phase) ? DOORS : s.today.getDayOfMonth();
        int d = upTo;
        if (RUNNING.equals(s.phase) && !m.entered.contains(d)) d--; // today's door isn't missed until midnight
        while (d >= 1 && m.entered.contains(d)) {
            m.streak++;
            d--;
        }
        int run = 0;
        for (int n = 1; n <= DOORS; n++) {
            run = m.entered.contains(n) ? run + 1 : 0;
            m.bestStreak = Math.max(m.bestStreak, run);
        }
        return m;
    }

    // ------------------------------------------------------------------ leaderboard

    /** The listed players, best first, ties sharing a rank: everyone who has chosen to be shown - and the asker. */
    List<StandingDto> standings(Season s, Long meId) {
        Map<Long, Integer> totals = new HashMap<>();
        for (DayResult r : s.results.values()) {
            r.byUser.forEach((userId, er) -> totals.merge(userId, er.points(), Integer::sum));
        }
        Map<Long, Boolean> hidden = new HashMap<>();
        profileRepository.findAllById(totals.keySet()).forEach(p -> hidden.put(p.getUserId(), !p.isShowName()));
        List<Long> listed = totals.keySet().stream()
                .filter(id -> id.equals(meId) || !hidden.getOrDefault(id, false))
                .collect(Collectors.toList());
        Map<Long, String> names = new HashMap<>();
        appUserRepository.findAllById(listed).forEach(u -> names.put(u.getId(), u.getName()));

        List<Long> ordered = new ArrayList<>(listed);
        ordered.sort(Comparator.<Long>comparingInt(totals::get).reversed()
                .thenComparing(id -> names.getOrDefault(id, "").toLowerCase(Locale.ROOT)));
        List<StandingDto> out = new ArrayList<>();
        for (int i = 0; i < ordered.size(); i++) {
            Long id = ordered.get(i);
            int points = totals.get(id);
            int rank = i > 0 && out.get(i - 1).points() == points ? out.get(i - 1).rank() : i + 1;
            out.add(new StandingDto(rank, names.getOrDefault(id, "?"), points, id.equals(meId)));
        }
        return out;
    }

    @Transactional(readOnly = true)
    public LeaderboardDto leaderboard(String email) {
        return leaderboard(email, clock.today());
    }

    @Transactional(readOnly = true)
    LeaderboardDto leaderboard(String email, LocalDate today) {
        Season s = load(seasonYear(today), today);
        if (BEFORE.equals(s.phase)) throw new IllegalStateException("Julekalenderen åpner 1. desember.");
        Long myId = findPlayer(email).map(AppUser::getId).orElse(null);
        List<StandingDto> all = standings(s, myId);
        List<StandingDto> shown = new ArrayList<>(all.stream().limit(LEADERBOARD_SIZE).collect(Collectors.toList()));
        StandingDto you = all.stream().filter(StandingDto::you).findFirst().orElse(null);
        if (you != null && !shown.contains(you)) shown.add(you);
        int gm = s.results.values().stream().mapToInt(r -> r.gamemasterPoints).sum();
        return new LeaderboardDto(shown, gm, s.results.size(), you == null ? null : you.rank(), all.size(), showName(myId));
    }

    // ------------------------------------------------------------------ one door

    @Transactional(readOnly = true)
    public DayDetailDto dayDetail(String email, int day) {
        return dayDetail(email, day, clock.today());
    }

    @Transactional(readOnly = true)
    DayDetailDto dayDetail(String email, int day, LocalDate today) {
        Season s = openSeason(day, today);
        return detail(s, day, findPlayer(email).map(AppUser::getId).orElse(null));
    }

    private Season openSeason(int day, LocalDate today) {
        if (day < 1 || day > DOORS) throw new ResourceNotFoundException("Fant ikke luke " + day + ".");
        Season s = load(seasonYear(today), today);
        if (BEFORE.equals(s.phase)) throw new IllegalStateException("Julekalenderen åpner 1. desember.");
        String status = s.status(day);
        if ("LOCKED".equals(status)) throw new IllegalStateException("Denne luka er ikke åpnet ennå.");
        if ("EMPTY".equals(status)) throw new ResourceNotFoundException("Det er ingen luke her.");
        return s;
    }

    private DayDetailDto detail(Season s, int n, Long myId) {
        AdventDay day = s.days.get(n);
        String status = s.status(n);
        boolean finale = AdventScoring.isFinale(n);
        List<AdventEntry> entries = s.entriesByDay.getOrDefault(day.getId(), List.of());
        AdventEntry mine = myId == null ? null : entries.stream().filter(e -> e.getUserId().equals(myId)).findFirst().orElse(null);
        EntryDto entry = mine == null ? null : new EntryDto(mine.getAnswers(), mine.isRisky(), mine.isJokerUsed());
        int usedElsewhere = myId == null ? 0 : (int) s.days.values().stream()
                .filter(d -> d.getDayNumber() != n)
                .flatMap(d -> s.entriesByDay.getOrDefault(d.getId(), List.of()).stream())
                .filter(e -> e.getUserId().equals(myId) && e.isJokerUsed()).count();
        int jokersLeft = Math.max(0, AdventScoring.JOKERS_PER_SEASON - usedElsewhere);

        if ("OPEN".equals(status)) {
            return new DayDetailDto(n, s.date(n), status, day.getCategory(), day.getType().name(), finale, entry, jokersLeft, !finale,
                    null, null, 0, 0, false, null);
        }

        DayResult result = s.results.get(n);
        Map<String, Integer> corrections = s.corrections.getOrDefault(day.getId(), Map.of());
        List<String> hidden = day.getAnswers();
        List<HiddenAnswerDto> hiddenDtos = new ArrayList<>();
        for (int i = 0; i < hidden.size(); i++) {
            int matched = result.matchedPlayers[i];
            hiddenDtos.add(new HiddenAnswerDto(hidden.get(i), matched, percent(matched, result.entrants),
                    day.getType() == AdventType.WILDCARD && Objects.equals(day.getWildcardIndex(), i)));
        }
        EntryResult er = myId == null ? null : result.byUser.get(myId);
        ResultDto resultDto = er == null ? null : new ResultDto(Arrays.stream(er.answerMatches()).boxed().collect(Collectors.toList()),
                er.matches(), er.wildcardHit(), er.basePoints(), er.jokerBonus(), er.points());
        return new DayDetailDto(n, s.date(n), status, day.getCategory(), day.getType().name(), finale, entry, jokersLeft, false,
                resultDto, hiddenDtos, result.entrants, result.gamemasterPoints, !day.isApproved(),
                crowd(entries, hidden, corrections, result.entrants));
    }

    private static int percent(int part, int whole) {
        return whole == 0 ? 0 : (int) Math.round(100.0 * part / whole);
    }

    /** What the players wrote most often (spelled the way most of them spelled it), and what each counted as. */
    static List<CrowdAnswerDto> crowd(List<AdventEntry> entries, List<String> hidden, Map<String, Integer> corrections, int entrants) {
        Map<String, Map<String, Integer>> spellings = new HashMap<>();
        Map<String, Integer> counts = new HashMap<>();
        for (AdventEntry e : entries) {
            Set<String> seen = new HashSet<>();
            for (String raw : e.getAnswers()) {
                String n = AdventScoring.normalize(raw);
                if (n.isEmpty() || !seen.add(n)) continue;
                counts.merge(n, 1, Integer::sum);
                spellings.computeIfAbsent(n, k -> new HashMap<>()).merge(raw.trim(), 1, Integer::sum);
            }
        }
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed().thenComparing(Map.Entry::getKey))
                .limit(CROWD_SIZE)
                .map(c -> {
                    String shown = spellings.get(c.getKey()).entrySet().stream()
                            .max(Map.Entry.<String, Integer>comparingByValue().thenComparing(Map.Entry.comparingByKey()))
                            .orElseThrow().getKey();
                    int idx = AdventScoring.matchIndex(shown, hidden, corrections);
                    return new CrowdAnswerDto(shown, c.getValue(), percent(c.getValue(), entrants), idx >= 0 ? idx : null);
                })
                .collect(Collectors.toList());
    }

    // ------------------------------------------------------------------ playing a door

    @Transactional
    public DayDetailDto submit(String email, int day, EntryRequest request) {
        return submit(email, day, request, clock.today());
    }

    @Transactional
    DayDetailDto submit(String email, int day, EntryRequest request, LocalDate today) {
        AppUser user = findPlayer(email).orElseThrow(() -> new IllegalStateException("Bare spillere kan delta i julekalenderen."));
        Season s = openSeason(day, today);
        if (!"OPEN".equals(s.status(day))) throw new IllegalStateException("Denne luka er stengt.");
        AdventDay d = s.days.get(day);
        // sent in is sent in: no changing the answers (or the joker or risk choice) afterwards
        if (entryRepository.findByDayIdAndUserId(d.getId(), user.getId()).isPresent()) {
            throw new IllegalStateException("Du har allerede sendt inn svarene dine for denne luka - de kan ikke endres.");
        }

        List<String> answers = cleanAnswers(request.answers());
        boolean finale = AdventScoring.isFinale(day);
        boolean useJoker = request.useJoker();
        if (useJoker && finale) throw new IllegalArgumentException("Jokeren kan ikke brukes på finalen.");
        if (useJoker) {
            long usedElsewhere = s.days.values().stream().filter(x -> x.getDayNumber() != day)
                    .flatMap(x -> s.entriesByDay.getOrDefault(x.getId(), List.of()).stream())
                    .filter(e -> e.getUserId().equals(user.getId()) && e.isJokerUsed()).count();
            if (usedElsewhere >= AdventScoring.JOKERS_PER_SEASON) throw new IllegalArgumentException("Du har brukt alle jokerne dine.");
        }

        AdventEntry entry = new AdventEntry();
        entry.setDayId(d.getId());
        entry.setUserId(user.getId());
        entry.setAnswers(answers);
        entry.setRisky(d.getType() == AdventType.RISK_REWARD && request.risky());
        entry.setJokerUsed(useJoker);
        entry.setSubmittedAt(java.time.Instant.now());
        AdventEntry saved = entryRepository.save(entry);

        // reflect the save in the loaded season, then answer from it
        s.entriesByDay.computeIfAbsent(d.getId(), k -> new ArrayList<>()).add(saved);
        return detail(s, day, user.getId());
    }

    /** Exactly four answers, none empty, no two the same once spelling differences are ignored. */
    static List<String> cleanAnswers(List<String> raw) {
        List<String> answers = (raw == null ? List.<String>of() : raw).stream()
                .map(a -> a == null ? "" : a.replaceAll("\\s+", " ").trim()).collect(Collectors.toList());
        if (answers.size() != 4 || answers.stream().anyMatch(a -> AdventScoring.normalize(a).isEmpty())) {
            throw new IllegalArgumentException("Du må skrive inn 4 svar.");
        }
        if (answers.stream().anyMatch(a -> a.length() > MAX_ANSWER_LENGTH)) {
            throw new IllegalArgumentException("Svarene kan ikke være lengre enn " + MAX_ANSWER_LENGTH + " tegn.");
        }
        if (answers.stream().map(AdventScoring::normalize).distinct().count() != 4) {
            throw new IllegalArgumentException("Du har skrevet det samme svaret to ganger - de 4 svarene må være ulike.");
        }
        return answers;
    }

    // ------------------------------------------------------------------ wrapped

    @Transactional(readOnly = true)
    public WrappedDto wrapped(String email) {
        return wrapped(email, clock.today());
    }

    @Transactional(readOnly = true)
    WrappedDto wrapped(String email, LocalDate today) {
        Season s = load(seasonYear(today), today);
        if (!FINISHED.equals(s.phase)) throw new IllegalStateException("Julekalender-wrapped er klar etter 24. desember.");
        Long myId = findPlayer(email).map(AppUser::getId).orElse(null);
        MyStats m = myStats(s, myId);
        List<StandingDto> all = standings(s, myId);
        Integer rank = all.stream().filter(StandingDto::you).map(StandingDto::rank).findFirst().orElse(null);

        BestDayDto best = null;
        int matchesTotal = 0, hiddenTotal = 0, jokerBonus = 0, risksTaken = 0, risksWon = 0, wildcardHits = 0, perfect = 0;
        for (Map.Entry<Integer, EntryResult> r : m.results.entrySet()) {
            AdventDay day = s.days.get(r.getKey());
            EntryResult er = r.getValue();
            if (best == null || er.points() > best.points()) best = new BestDayDto(r.getKey(), day.getCategory(), er.points());
            matchesTotal += er.matches();
            hiddenTotal += day.getAnswers().size();
            jokerBonus += er.jokerBonus();
            if (day.getType() == AdventType.RISK_REWARD && er.entry().isRisky()) {
                risksTaken++;
                if (er.matches() >= 1) risksWon++;
            }
            if (er.wildcardHit()) wildcardHits++;
            if (day.getType() != AdventType.ONE_OF_FOUR && er.matches() == 4) perfect++;
        }
        int daysTotal = (int) s.days.values().stream().filter(AdventDay::isReady).count();
        int gm = s.results.values().stream().mapToInt(r -> r.gamemasterPoints).sum();
        List<StandingDto> podium = all.stream().filter(p -> p.rank() <= 3).limit(5).collect(Collectors.toList());
        return new WrappedDto(s.year, m.total, rank, all.size(), m.results.size(), daysTotal, best, m.bestStreak,
                percent(matchesTotal, hiddenTotal), matchesTotal, m.jokersUsed, jokerBonus, risksTaken, risksWon, wildcardHits,
                perfect, gm, podium);
    }
}
