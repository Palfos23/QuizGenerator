package com.quizapp.service;

import com.quizapp.dto.LeagueDtos.*;
import com.quizapp.model.LeagueFormat;
import com.quizapp.model.LeagueMatch;
import com.quizapp.model.LeagueSeason;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Works out everything about one league season - table, matchups, playoffs, stats, recap - from three stored
 * things: who takes part, the drawn matchups (head-to-head), and the players' day scores. Nothing it returns is
 * stored, so a late admin decision on an answer, or a day finishing, updates every view by itself.
 *
 * The rules:
 * - A day's score counts only if the quiz was handed in that day (enforced where scores are recorded); a day not
 *   handed in by its end counts as 0. While answers wait for an admin, that day reads "pending" and any matchup
 *   on it waits.
 * - Head-to-head: the higher score on the day wins - 3 points, a draw 1 each. If neither handed in, both lose.
 *   The table is sorted by points, then total quiz score, then wins.
 * - Playoffs (head-to-head, if chosen and at least four players): the day after the regular season the top four
 *   meet, 1 v 4 and 2 v 3; the day after that the winners play the final and the losers for third place. A draw,
 *   or neither handing in, sends the better seed through. The pairings need not be known before the day: everyone
 *   plays the same daily quiz, so the result is simply the two players' scores on that day.
 * - Points: the total of every day's score in the season, then the average.
 *
 * Pure: give it an Input, get a SeasonView. No repositories, no clock - `today` is part of the input.
 */
public final class LeagueStandings {

    private LeagueStandings() {
    }

    /** What the computation needs. scores: user -> day -> final score; pending: userId + "|" + date of answers in review. */
    public static final class Input {
        public LeagueSeason season;
        public List<Long> participants = List.of();
        public Map<Long, String> names = Map.of();
        public Set<Long> left = Set.of();
        public Long meId;
        public Long expertId;
        public List<LeagueMatch> matches = List.of();
        public Map<Long, Map<LocalDate, Integer>> scores = Map.of();
        public Set<String> pending = Set.of();
        public LocalDate today;
        public String notice;
    }

    public static String pendingKey(Long userId, LocalDate date) {
        return userId + "|" + date;
    }

    // Points-league days shown day by day (an open-ended season could otherwise grow without end)
    static final int POINTS_DAYS_SHOWN = 14;
    private static final int FORM_LENGTH = 5;

    enum DayState { SCORED, PENDING, MISSED, NOT_YET, UPCOMING }

    /** One matchup, evaluated. b == null && bExpert: against the Expert. a/b null otherwise: not known yet (playoffs). */
    static final class Eval {
        LocalDate date;
        String stage;
        Long a, b;
        boolean bExpert;
        Integer seedA, seedB;
        String placeholderA, placeholderB;
        DayState sa, sb;
        Integer va, vb;
        String status;
        String outcome;
        String advancing;

        Long winner() {
            if ("A".equals(advancing)) return a;
            if ("B".equals(advancing)) return b;
            return null;
        }

        Long loser() {
            if ("A".equals(advancing)) return b;
            if ("B".equals(advancing)) return a;
            return null;
        }

        boolean involves(Long id) {
            return id != null && (id.equals(a) || (!bExpert && id.equals(b)));
        }
    }

    /** Running totals for one player. */
    private static final class Acc {
        final Long id;
        int played, total, wins, draws, losses, points, beatExpert, currentStreak, longestStreak;
        Integer best;
        final List<String> form = new ArrayList<>();

        Acc(Long id) {
            this.id = id;
        }

        Double average() {
            return played == 0 ? null : Math.round(total * 10.0 / played) / 10.0;
        }
    }

    public static SeasonView compute(Input in) {
        return new LeagueStandings.Run(in).view();
    }

    /** One computation: the input plus everything worked out along the way. */
    private static final class Run {
        final Input in;
        final LeagueSeason s;
        final boolean h2h;
        final LocalDate today;
        final String state;
        final LocalDate lastCounted; // last day that counts so far (null before the start)
        final Map<Long, Acc> acc = new LinkedHashMap<>();
        final List<Eval> regular = new ArrayList<>();
        final List<Eval> playoffEvals = new ArrayList<>();
        List<Acc> order = new ArrayList<>();
        Long champion, runnerUp, third;
        boolean playoffsOn;

        Run(Input in) {
            this.in = in;
            this.s = in.season;
            this.h2h = s.getFormat() == LeagueFormat.HEAD_TO_HEAD;
            this.today = in.today;
            if (today.isBefore(s.getStartDate())) state = "UPCOMING";
            else if (s.getEndDate() != null && today.isAfter(s.getEndDate())) state = "FINISHED";
            else state = "ACTIVE";
            if ("UPCOMING".equals(state)) lastCounted = null;
            else lastCounted = s.getEndDate() != null && s.getEndDate().isBefore(today) ? s.getEndDate() : today;
            for (Long p : in.participants) acc.put(p, new Acc(p));
            playoffsOn = h2h && s.isPlayoffs() && in.participants.size() >= LeagueSeason.PLAYOFF_SIZE;
        }

        // ---------- days and matchups ----------

        DayState state(Long user, LocalDate d) {
            if (user != null) {
                Integer score = in.scores.getOrDefault(user, Map.of()).get(d);
                if (score != null) return DayState.SCORED;
                if (in.pending.contains(pendingKey(user, d))) return DayState.PENDING;
            }
            if (d.isBefore(today)) return DayState.MISSED;
            if (d.equals(today)) return DayState.NOT_YET;
            return DayState.UPCOMING;
        }

        Integer score(Long user, LocalDate d) {
            return user == null ? null : in.scores.getOrDefault(user, Map.of()).get(d);
        }

        boolean playedOrPlaying(Long user, LocalDate d) {
            DayState st = state(user, d);
            return st == DayState.SCORED || st == DayState.PENDING;
        }

        Eval evaluate(Long a, Long b, boolean bExpert, LocalDate d, String stage) {
            Eval e = new Eval();
            e.date = d;
            e.stage = stage;
            e.a = a;
            e.b = bExpert ? null : b;
            e.bExpert = bExpert;
            Long bUser = bExpert ? in.expertId : b;
            e.sa = state(a, d);
            e.sb = state(bUser, d);
            e.va = e.sa == DayState.SCORED ? score(a, d) : null;
            e.vb = e.sb == DayState.SCORED ? score(bUser, d) : null;
            if (d.isAfter(today)) e.status = "UPCOMING";
            else if (e.sa == DayState.PENDING || e.sb == DayState.PENDING) e.status = "PENDING";
            else if (e.sa == DayState.NOT_YET || e.sb == DayState.NOT_YET) e.status = "LIVE";
            else e.status = "FINAL";
            if ("FINAL".equals(e.status)) {
                if (e.sa == DayState.MISSED && e.sb == DayState.MISSED) {
                    e.outcome = "BOTH_LOST";
                } else {
                    int x = e.va == null ? 0 : e.va;
                    int y = e.vb == null ? 0 : e.vb;
                    e.outcome = x > y ? "A" : x < y ? "B" : "DRAW";
                }
            }
            return e;
        }

        /** A playoff matchup; a side may not be known yet (its placeholder says whose it will be). A tie goes to side A, the better seed. */
        Eval playoff(Long a, Long b, Integer seedA, Integer seedB, String phA, String phB, LocalDate d, String stage) {
            Eval e;
            if (a != null && b != null) {
                e = evaluate(a, b, false, d, stage);
                if ("FINAL".equals(e.status)) e.advancing = "B".equals(e.outcome) ? "B" : "A";
            } else {
                e = new Eval();
                e.date = d;
                e.stage = stage;
                e.a = a;
                e.b = b;
                e.sa = a == null ? null : state(a, d);
                e.sb = b == null ? null : state(b, d);
                e.va = e.sa == DayState.SCORED ? score(a, d) : null;
                e.vb = e.sb == DayState.SCORED ? score(b, d) : null;
                e.status = d.isAfter(today) ? "UPCOMING" : "PENDING";
            }
            e.seedA = seedA;
            e.seedB = seedB;
            if (a == null) e.placeholderA = phA;
            if (b == null) e.placeholderB = phB;
            return e;
        }

        // ---------- the whole season ----------

        SeasonView view() {
            countDays();
            if (h2h) {
                in.matches.stream()
                        .sorted(Comparator.comparing(LeagueMatch::getQuizDate).thenComparing(m -> m.getId() == null ? 0L : m.getId()))
                        .forEach(m -> regular.add(evaluate(m.getUserA(), m.getUserB(), m.getUserB() == null, m.getQuizDate(), "REGULAR")));
                for (Eval e : regular) applyResult(e, true);
                order = sortedH2h();
                if (playoffsOn && s.isScheduled()) buildPlayoffs();
            } else {
                order = sortedPoints();
            }
            decideChampion();

            List<TableRow> table = tableRows();
            boolean anyPending = regular.stream().anyMatch(e -> "PENDING".equals(e.status) && e.a != null)
                    || playoffEvals.stream().anyMatch(e -> "PENDING".equals(e.status))
                    || pendingInSeason();
            boolean settled = "FINISHED".equals(state) && !anyPending;

            return new SeasonView(s.getNumber(), s.getFormat(), s.getStartDate(), s.getEndDate(), s.regularEndDate(),
                    s.getRegularWeeks(), playoffsOn || (h2h && s.isPlayoffs() && !s.isScheduled()), state, settled,
                    s.isScheduled(), in.notice, in.participants.stream().map(this::ref).collect(Collectors.toList()),
                    table, days(), statRows(), "FINISHED".equals(state) ? recap(table, !settled) : null, todayView());
        }

        boolean pendingInSeason() {
            if (lastCounted == null) return false;
            for (Long p : in.participants) {
                for (LocalDate d = s.getStartDate(); !d.isAfter(lastCounted); d = d.plusDays(1)) {
                    if (in.pending.contains(pendingKey(p, d))) return true;
                }
            }
            return false;
        }

        /** Played days, totals, best day, streaks and "beat the Expert" - over every day that counts so far. */
        void countDays() {
            if (lastCounted == null) return;
            // Head-to-head totals cover the regular season only (that's what the table ranks on)
            LocalDate tableEnd = h2h && s.regularEndDate() != null && s.regularEndDate().isBefore(lastCounted) ? s.regularEndDate() : lastCounted;
            for (Acc a : acc.values()) {
                int run = 0;
                List<String> pointsForm = new ArrayList<>();
                for (LocalDate d = s.getStartDate(); !d.isAfter(lastCounted); d = d.plusDays(1)) {
                    DayState st = state(a.id, d);
                    if (st == DayState.SCORED || st == DayState.PENDING) {
                        run++;
                        a.longestStreak = Math.max(a.longestStreak, run);
                    } else if (st != DayState.NOT_YET) {
                        run = 0;
                    }
                    if (st == DayState.SCORED) {
                        int v = score(a.id, d);
                        if (!d.isAfter(tableEnd)) {
                            a.played++;
                            a.total += v;
                            a.best = a.best == null ? v : Math.max(a.best, v);
                        }
                        Integer ex = in.expertId == null || in.expertId.equals(a.id) ? null : score(in.expertId, d);
                        if (ex != null && v > ex) a.beatExpert++;
                    }
                    if (!h2h && st != DayState.NOT_YET) {
                        pointsForm.add(st == DayState.SCORED ? String.valueOf(score(a.id, d)) : st == DayState.PENDING ? "…" : "-");
                    }
                }
                a.currentStreak = run;
                if (!h2h) a.form.addAll(pointsForm.subList(Math.max(0, pointsForm.size() - FORM_LENGTH), pointsForm.size()));
            }
        }

        void applyResult(Eval e, boolean countInTable) {
            if (!"FINAL".equals(e.status)) return;
            Acc a = acc.get(e.a);
            Acc b = e.bExpert ? null : acc.get(e.b);
            String oa = switch (e.outcome) {
                case "A" -> "W";
                case "DRAW" -> "D";
                default -> "L";
            };
            String ob = switch (e.outcome) {
                case "B" -> "W";
                case "DRAW" -> "D";
                default -> "L";
            };
            if (a != null && countInTable) record(a, oa);
            if (b != null && countInTable) record(b, ob);
        }

        void record(Acc a, String result) {
            switch (result) {
                case "W" -> { a.wins++; a.points += 3; }
                case "D" -> { a.draws++; a.points += 1; }
                default -> a.losses++;
            }
            a.form.add(result);
            if (a.form.size() > FORM_LENGTH) a.form.remove(0);
        }

        List<Acc> sortedH2h() {
            return acc.values().stream().sorted(Comparator
                    .comparingInt((Acc a) -> -a.points)
                    .thenComparingInt(a -> -a.total)
                    .thenComparingInt(a -> -a.wins)
                    .thenComparing(a -> sortName(a.id))).collect(Collectors.toList());
        }

        List<Acc> sortedPoints() {
            return acc.values().stream().sorted(Comparator
                    .comparingInt((Acc a) -> -a.total)
                    .thenComparing(a -> -(a.average() == null ? 0 : a.average()))
                    .thenComparing(a -> sortName(a.id))).collect(Collectors.toList());
        }

        String sortName(Long id) {
            String n = in.names.get(id);
            return n == null ? "￿" : n.toLowerCase(Locale.ROOT);
        }

        boolean sameRank(Acc x, Acc y) {
            return h2h ? x.points == y.points && x.total == y.total : x.total == y.total;
        }

        void buildPlayoffs() {
            LocalDate semi = s.regularEndDate().plusDays(1);
            LocalDate fin = semi.plusDays(1);
            boolean seedsKnown = today.isAfter(s.regularEndDate());
            List<Long> seeds = order.stream().limit(LeagueSeason.PLAYOFF_SIZE).map(a -> a.id).collect(Collectors.toList());
            Long s1 = seedsKnown ? seeds.get(0) : null, s2 = seedsKnown ? seeds.get(1) : null;
            Long s3 = seedsKnown ? seeds.get(2) : null, s4 = seedsKnown ? seeds.get(3) : null;
            Eval semi1 = playoff(s1, s4, 1, 4, "SEED_1", "SEED_4", semi, "SEMI");
            Eval semi2 = playoff(s2, s3, 2, 3, "SEED_2", "SEED_3", semi, "SEMI");
            Integer w1Seed = semi1.winner() == null ? null : ("A".equals(semi1.advancing) ? 1 : 4);
            Integer w2Seed = semi2.winner() == null ? null : ("A".equals(semi2.advancing) ? 2 : 3);
            Integer l1Seed = semi1.loser() == null ? null : ("A".equals(semi1.advancing) ? 4 : 1);
            Integer l2Seed = semi2.loser() == null ? null : ("A".equals(semi2.advancing) ? 3 : 2);
            // The better seed is side A, so a tie in the final goes to them
            Eval finalMatch = orderedPlayoff(semi1.winner(), w1Seed, "WINNER_SEMI_1", semi2.winner(), w2Seed, "WINNER_SEMI_2", fin, "FINAL");
            Eval bronze = orderedPlayoff(semi1.loser(), l1Seed, "LOSER_SEMI_1", semi2.loser(), l2Seed, "LOSER_SEMI_2", fin, "BRONZE");
            playoffEvals.addAll(List.of(semi1, semi2, finalMatch, bronze));
        }

        Eval orderedPlayoff(Long x, Integer xs, String xph, Long y, Integer ys, String yph, LocalDate d, String stage) {
            boolean swap = xs != null && ys != null && ys < xs;
            return swap ? playoff(y, x, ys, xs, yph, xph, d, stage) : playoff(x, y, xs, ys, xph, yph, d, stage);
        }

        void decideChampion() {
            if (!"FINISHED".equals(state)) return;
            if (playoffsOn && s.isScheduled() && playoffEvals.size() == 4) {
                Eval fin = playoffEvals.get(2);
                Eval bronze = playoffEvals.get(3);
                champion = fin.winner();
                runnerUp = fin.loser();
                third = bronze.winner();
            } else {
                champion = order.size() > 0 ? order.get(0).id : null;
                runnerUp = order.size() > 1 ? order.get(1).id : null;
                third = order.size() > 2 ? order.get(2).id : null;
            }
        }

        // ---------- views ----------

        PlayerRef ref(Long id) {
            if (id == null) return null;
            boolean expert = id.equals(in.expertId) && !in.participants.contains(id);
            return new PlayerRef(id, expert ? null : in.names.get(id), id.equals(in.meId), expert, in.left.contains(id));
        }

        MatchView view(Eval e) {
            PlayerRef b = e.bExpert ? new PlayerRef(in.expertId, null, false, true, false) : ref(e.b);
            return new MatchView(e.date, e.stage, ref(e.a), b, e.va, e.vb,
                    e.sa == null ? null : e.sa.name(), e.sb == null ? null : e.sb.name(), e.status, e.outcome,
                    e.seedA, e.seedB, e.placeholderA, e.placeholderB, e.advancing);
        }

        List<TableRow> tableRows() {
            List<TableRow> rows = new ArrayList<>();
            int rank = 0;
            Acc prev = null;
            for (int i = 0; i < order.size(); i++) {
                Acc a = order.get(i);
                if (prev == null || !sameRank(prev, a)) rank = i + 1;
                prev = a;
                rows.add(new TableRow(rank, ref(a.id), a.played, a.wins, a.draws, a.losses, a.points, a.total,
                        a.average(), a.best, List.copyOf(a.form)));
            }
            return rows;
        }

        List<StatRow> statRows() {
            return order.stream().map(a -> new StatRow(ref(a.id), a.played, a.average(), a.best, a.wins, a.draws, a.losses,
                    a.beatExpert, a.currentStreak, a.longestStreak)).collect(Collectors.toList());
        }

        List<DayView> days() {
            List<DayView> out = new ArrayList<>();
            if (h2h) {
                Map<LocalDate, List<Eval>> byDay = regular.stream().collect(Collectors.groupingBy(e -> e.date, TreeMap::new, Collectors.toList()));
                byDay.forEach((d, list) -> {
                    Set<Long> busy = new HashSet<>();
                    list.forEach(e -> { busy.add(e.a); if (e.b != null) busy.add(e.b); });
                    List<PlayerRef> byes = in.participants.stream().filter(p -> !busy.contains(p)).map(this::ref).collect(Collectors.toList());
                    out.add(new DayView(d, "REGULAR", list.stream().map(this::view).collect(Collectors.toList()), byes, List.of()));
                });
                if (!playoffEvals.isEmpty()) {
                    out.add(new DayView(playoffEvals.get(0).date, "SEMI", List.of(view(playoffEvals.get(0)), view(playoffEvals.get(1))), List.of(), List.of()));
                    out.add(new DayView(playoffEvals.get(2).date, "FINAL", List.of(view(playoffEvals.get(2)), view(playoffEvals.get(3))), List.of(), List.of()));
                }
            } else if (lastCounted != null) {
                LocalDate from = lastCounted.minusDays(POINTS_DAYS_SHOWN - 1);
                if (from.isBefore(s.getStartDate())) from = s.getStartDate();
                for (LocalDate d = from; !d.isAfter(lastCounted); d = d.plusDays(1)) {
                    LocalDate day = d;
                    List<DayScore> scores = order.stream()
                            .map(a -> new DayScore(ref(a.id), state(a.id, day).name(), score(a.id, day)))
                            .sorted(Comparator.comparingInt((DayScore x) -> -(x.score() == null ? -1 : x.score())))
                            .collect(Collectors.toList());
                    out.add(new DayView(day, "DAY", List.of(), List.of(), scores));
                }
            }
            return out;
        }

        TodayView todayView() {
            if (!"ACTIVE".equals(state)) return null;
            MatchView mine = null;
            if (in.meId != null) {
                for (Eval e : allEvals()) {
                    if (e.date.equals(today) && e.involves(in.meId)) {
                        mine = view(e);
                        break;
                    }
                }
            }
            int played = (int) in.participants.stream().filter(p -> playedOrPlaying(p, today)).count();
            boolean iPlayed = in.meId != null && playedOrPlaying(in.meId, today);
            return new TodayView(today, mine, iPlayed, played, in.participants.size());
        }

        List<Eval> allEvals() {
            List<Eval> all = new ArrayList<>(regular);
            all.addAll(playoffEvals);
            return all;
        }

        RecapView recap(List<TableRow> table, boolean provisional) {
            List<Highlight> hl = new ArrayList<>();
            // Best single day
            Long bestUser = null;
            Integer bestScore = null;
            LocalDate bestDate = null;
            for (LocalDate d = s.getStartDate(); lastCounted != null && !d.isAfter(lastCounted); d = d.plusDays(1)) {
                for (Long p : in.participants) {
                    Integer v = score(p, d);
                    if (v != null && (bestScore == null || v > bestScore)) {
                        bestScore = v;
                        bestUser = p;
                        bestDate = d;
                    }
                }
            }
            if (bestUser != null) hl.add(new Highlight("BEST_DAY", ref(bestUser), null, bestScore, null, null, bestDate));

            int seasonDays = lastCounted == null ? 0 : (int) (lastCounted.toEpochDay() - s.getStartDate().toEpochDay() + 1);
            int minDays = Math.max(1, Math.min(3, seasonDays));
            acc.values().stream().filter(a -> a.played >= minDays && a.average() != null)
                    .max(Comparator.comparingDouble(Acc::average))
                    .ifPresent(a -> hl.add(new Highlight("BEST_AVERAGE", ref(a.id), null, a.played, null, a.average(), null)));
            acc.values().stream().max(Comparator.comparingInt(a -> a.played)).filter(a -> a.played > 0)
                    .ifPresent(a -> hl.add(new Highlight("MOST_PLAYED", ref(a.id), null, a.played, seasonDays, null, null)));

            if (h2h) {
                acc.values().stream().max(Comparator.comparingInt(a -> a.wins)).filter(a -> a.wins > 0)
                        .ifPresent(a -> hl.add(new Highlight("MOST_WINS", ref(a.id), null, a.wins, null, null, null)));
                // Longest run of wins, over every matchup of the season (playoffs too)
                Map<Long, int[]> streak = new HashMap<>();
                Long streakUser = null;
                int streakBest = 0;
                Eval biggest = null;
                int biggestMargin = 0;
                for (Eval e : allEvals()) {
                    if (!"FINAL".equals(e.status)) continue;
                    for (Long side : new Long[]{e.a, e.bExpert ? null : e.b}) {
                        if (side == null || !acc.containsKey(side)) continue;
                        boolean won = side.equals(e.a) ? "A".equals(e.outcome) : "B".equals(e.outcome);
                        int[] run = streak.computeIfAbsent(side, k -> new int[1]);
                        run[0] = won ? run[0] + 1 : 0;
                        if (run[0] > streakBest) {
                            streakBest = run[0];
                            streakUser = side;
                        }
                    }
                    int x = e.va == null ? 0 : e.va, y = e.vb == null ? 0 : e.vb;
                    boolean memberWon = "A".equals(e.outcome) || ("B".equals(e.outcome) && !e.bExpert);
                    if (memberWon && Math.abs(x - y) > biggestMargin) {
                        biggestMargin = Math.abs(x - y);
                        biggest = e;
                    }
                }
                if (streakUser != null && streakBest > 1) hl.add(new Highlight("LONGEST_WIN_STREAK", ref(streakUser), null, streakBest, null, null, null));
                if (biggest != null) {
                    boolean aWon = "A".equals(biggest.outcome);
                    PlayerRef loser = aWon ? (biggest.bExpert ? new PlayerRef(in.expertId, null, false, true, false) : ref(biggest.b)) : ref(biggest.a);
                    int wv = aWon ? nz(biggest.va) : nz(biggest.vb), lv = aWon ? nz(biggest.vb) : nz(biggest.va);
                    hl.add(new Highlight("BIGGEST_WIN", ref(aWon ? biggest.a : biggest.b), loser, wv, lv, null, biggest.date));
                }
            }
            if (in.expertId != null) {
                acc.values().stream().max(Comparator.comparingInt(a -> a.beatExpert)).filter(a -> a.beatExpert > 0)
                        .ifPresent(a -> hl.add(new Highlight("BEAT_EXPERT", ref(a.id), null, a.beatExpert, null, null, null)));
            }

            TableRow myRow = table.stream().filter(r -> r.player().me()).findFirst().orElse(null);
            StatRow myStats = statRows().stream().filter(r -> r.player().me()).findFirst().orElse(null);
            return new RecapView(provisional, ref(champion), ref(runnerUp), ref(third), hl, myRow, myStats, in.participants.size());
        }

        int nz(Integer v) {
            return v == null ? 0 : v;
        }
    }

    // ---------- the draw ----------

    /** Sentinel for "the Expert" in a drawn pairing (stored as userB = null). */
    static final long EXPERT = -1L;
    /** Sentinel for "nobody" - the odd one out sits the day out (no Expert configured). */
    static final long BYE = -2L;

    /**
     * Round-robin pairings for `days` days: everyone meets everyone once per cycle of (n - 1) days (the circle
     * method), and the cycle repeats for longer seasons. An odd player out meets the Expert when there is one,
     * otherwise sits the day out. Returns one list of [a, b] pairs per day; b may be EXPERT, BYE pairs are left out.
     */
    static List<List<long[]>> roundRobin(List<Long> players, int days, boolean withExpert) {
        List<Long> slots = new ArrayList<>(players);
        if (slots.size() % 2 == 1) slots.add(withExpert ? EXPERT : BYE);
        int n = slots.size();
        List<List<long[]>> out = new ArrayList<>();
        if (n < 2) return out;
        int rounds = n - 1;
        for (int day = 0; day < days; day++) {
            int r = day % rounds;
            // rotate every slot but the first r steps
            List<Long> rot = new ArrayList<>(n);
            rot.add(slots.get(0));
            for (int i = 0; i < n - 1; i++) rot.add(slots.get(1 + Math.floorMod(i - r, n - 1)));
            List<long[]> pairs = new ArrayList<>();
            for (int i = 0; i < n / 2; i++) {
                long x = rot.get(i), y = rot.get(n - 1 - i);
                if (x == BYE || y == BYE) continue;
                // keep the Expert always on side b; otherwise alternate sides so nobody is always "home"
                if (x == EXPERT) pairs.add(new long[]{y, x});
                else if (y == EXPERT) pairs.add(new long[]{x, y});
                else pairs.add((day + i) % 2 == 0 ? new long[]{x, y} : new long[]{y, x});
            }
            out.add(pairs);
        }
        return out;
    }
}
