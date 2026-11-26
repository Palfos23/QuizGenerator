package com.quizapp.service;

import com.quizapp.config.RequestLang;
import com.quizapp.dto.LeagueDtos.*;
import com.quizapp.exception.ResourceNotFoundException;
import com.quizapp.model.*;
import com.quizapp.repository.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Private daily-quiz leagues: creating and joining them, their seasons, and everything the league pages show.
 * The competition itself is worked out by LeagueStandings from stored day scores (LeagueScoreService) and,
 * for head-to-head, the matchups drawn here on a season's first day.
 */
@Service
public class LeagueService {

    static final int MAX_MEMBERS = 50;
    static final int MAX_NAME_LENGTH = 40;
    static final int MAX_H2H_WEEKS = 12;
    static final int MAX_POINTS_WEEKS = 52;
    // How far ahead a season may be set to start
    static final int MAX_DAYS_AHEAD = 90;
    // A finished season's recap is offered on the dashboard for this long
    static final int RECAP_DAYS = 14;
    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 6;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final LeagueRepository leagueRepository;
    private final LeagueSeasonRepository seasonRepository;
    private final LeagueMemberRepository memberRepository;
    private final LeagueMatchRepository matchRepository;
    private final LeagueDayScoreRepository dayScoreRepository;
    private final DailyQuizAttemptRepository attemptRepository;
    private final AppUserRepository appUserRepository;
    private final LeagueScoreService leagueScoreService;
    private final ExpertConfig expertConfig;

    // Tests move the clock; everything else uses the same "today" as the daily quiz (LocalDate.now()).
    private Clock clock = Clock.systemDefaultZone();

    public LeagueService(LeagueRepository leagueRepository, LeagueSeasonRepository seasonRepository,
                         LeagueMemberRepository memberRepository, LeagueMatchRepository matchRepository,
                         LeagueDayScoreRepository dayScoreRepository, DailyQuizAttemptRepository attemptRepository,
                         AppUserRepository appUserRepository, LeagueScoreService leagueScoreService,
                         ExpertConfig expertConfig) {
        this.leagueRepository = leagueRepository;
        this.seasonRepository = seasonRepository;
        this.memberRepository = memberRepository;
        this.matchRepository = matchRepository;
        this.dayScoreRepository = dayScoreRepository;
        this.attemptRepository = attemptRepository;
        this.appUserRepository = appUserRepository;
        this.leagueScoreService = leagueScoreService;
        this.expertConfig = expertConfig;
    }

    void setClock(Clock clock) {
        this.clock = clock;
    }

    LocalDate today() {
        return LocalDate.now(clock);
    }

    // ---------- creating, joining, leaving ----------

    @Transactional
    public LeagueDetail create(String email, CreateLeagueRequest request) {
        AppUser me = player(email);
        String name = request.name() == null ? "" : request.name().trim();
        if (name.isEmpty() || name.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException(RequestLang.t("Gi ligaen et navn på 1-40 tegn.", "Give the league a name of 1-40 characters."));
        }
        League league = new League();
        league.setName(name);
        league.setCode(newCode());
        league.setOwnerUserId(me.getId());
        leagueRepository.save(league);
        memberRepository.save(new LeagueMember(league, me.getId()));
        seasonRepository.save(newSeason(league, 1, request.settings()));
        leagueScoreService.syncUser(email);
        return detail(email, league.getId(), null);
    }

    @Transactional
    public JoinPreview preview(String email, String code) {
        AppUser me = player(email);
        League league = byCode(code);
        LeagueSeason season = latestSeason(league);
        ensureScheduled(season);
        Optional<LeagueMember> mine = memberRepository.findByLeague_IdAndUserId(league.getId(), me.getId());
        boolean already = mine.map(LeagueMember::isActive).orElse(false);
        boolean joinsCurrent = season.getFormat() != LeagueFormat.HEAD_TO_HEAD || !season.isScheduled();
        return new JoinPreview(league.getId(), league.getName(), league.getCode(), season.getFormat(), stateOf(season),
                season.getStartDate(), season.getEndDate(), (int) memberRepository.countByLeague_IdAndLeftAtIsNull(league.getId()),
                already, joinsCurrent);
    }

    @Transactional
    public LeagueDetail join(String email, String code) {
        AppUser me = player(email);
        League league = byCode(code);
        Optional<LeagueMember> existing = memberRepository.findByLeague_IdAndUserId(league.getId(), me.getId());
        if (existing.isPresent() && existing.get().isActive()) {
            return detail(email, league.getId(), null);
        }
        if (memberRepository.countByLeague_IdAndLeftAtIsNull(league.getId()) >= MAX_MEMBERS) {
            throw new IllegalStateException(RequestLang.t("Ligaen er full (maks 50 spillere).", "The league is full (50 players at most)."));
        }
        if (existing.isPresent()) {
            existing.get().setLeftAt(null);
            memberRepository.save(existing.get());
        } else {
            memberRepository.save(new LeagueMember(league, me.getId()));
        }
        leagueScoreService.syncUser(email);
        return detail(email, league.getId(), null);
    }

    @Transactional
    public void leave(String email, Long leagueId) {
        AppUser me = player(email);
        League league = memberLeague(me, leagueId);
        LeagueMember member = memberRepository.findByLeague_IdAndUserId(leagueId, me.getId()).orElseThrow();
        member.setLeftAt(Instant.now(clock));
        memberRepository.save(member);
        if (league.getOwnerUserId().equals(me.getId())) {
            handOverOrDelete(league);
        }
    }

    @Transactional
    public LeagueDetail removeMember(String email, Long leagueId, Long userId) {
        AppUser me = player(email);
        League league = ownedLeague(me, leagueId);
        if (userId.equals(me.getId())) {
            throw new IllegalArgumentException(RequestLang.t("Bruk «Forlat ligaen» for å gå ut selv.", "Use \"Leave league\" to leave it yourself."));
        }
        LeagueMember member = memberRepository.findByLeague_IdAndUserId(league.getId(), userId)
                .filter(LeagueMember::isActive)
                .orElseThrow(() -> new ResourceNotFoundException(RequestLang.t("Fant ikke spilleren i ligaen.", "That player isn't in the league.")));
        member.setLeftAt(Instant.now(clock));
        memberRepository.save(member);
        return detail(email, leagueId, null);
    }

    @Transactional
    public void delete(String email, Long leagueId) {
        AppUser me = player(email);
        deleteLeague(ownedLeague(me, leagueId));
    }

    /**
     * The owner starts the next season. Allowed when the current one is over, before it has started (the settings
     * are simply replaced), or for a points season without an end (it ends yesterday).
     */
    @Transactional
    public LeagueDetail newSeason(String email, Long leagueId, SeasonSettings settings) {
        AppUser me = player(email);
        League league = ownedLeague(me, leagueId);
        LeagueSeason latest = latestSeason(league);
        String state = stateOf(latest);
        int number = latest.getNumber() + 1;
        if ("UPCOMING".equals(state)) {
            number = latest.getNumber();
            removeSeason(latest);
        } else if ("ACTIVE".equals(state)) {
            if (latest.getFormat() != LeagueFormat.POINTS || latest.getEndDate() != null) {
                throw new IllegalStateException(RequestLang.t("Sesongen pågår fortsatt.", "The season is still running."));
            }
            LocalDate yesterday = today().minusDays(1);
            if (yesterday.isBefore(latest.getStartDate())) {
                number = latest.getNumber();
                removeSeason(latest);
            } else {
                latest.setEndDate(yesterday);
                seasonRepository.save(latest);
            }
        }
        seasonRepository.save(newSeason(league, number, settings));
        return detail(email, leagueId, null);
    }

    // ---------- reading ----------

    @Transactional
    public List<LeagueCard> myLeagues(String email) {
        AppUser me = player(email);
        LocalDate today = today();
        List<LeagueCard> cards = new ArrayList<>();
        for (LeagueMember m : memberRepository.findByUserIdAndLeftAtIsNull(me.getId())) {
            League league = m.getLeague();
            LeagueSeason season = latestSeason(league);
            ensureScheduled(season);
            SeasonView v = seasonView(league, season, me.getId());
            Integer myRank = v.table().stream().filter(r -> r.player().me()).map(TableRow::rank).findFirst().orElse(null);
            PlayerRef opponent = null;
            boolean opponentPlayed = false;
            if (v.today() != null && v.today().myMatch() != null) {
                MatchView mm = v.today().myMatch();
                boolean iAmA = mm.a() != null && mm.a().me();
                opponent = iAmA ? mm.b() : mm.a();
                String st = iAmA ? mm.stateB() : mm.stateA();
                opponentPlayed = "SCORED".equals(st) || "PENDING".equals(st);
            }
            boolean recapReady = "FINISHED".equals(v.state()) && v.endDate() != null && !v.endDate().isBefore(today.minusDays(RECAP_DAYS));
            cards.add(new LeagueCard(league.getId(), league.getName(), season.getFormat(), season.getNumber(), v.state(),
                    season.getStartDate(), season.getEndDate(), (int) memberRepository.countByLeague_IdAndLeftAtIsNull(league.getId()),
                    v.participants().size(), v.participants().stream().anyMatch(PlayerRef::me), myRank, opponent, opponentPlayed,
                    v.today() != null && v.today().iPlayed(), v.today() == null ? 0 : v.today().playedCount(), recapReady,
                    v.recap() == null ? null : v.recap().champion()));
        }
        Map<String, Integer> stateOrder = Map.of("ACTIVE", 0, "UPCOMING", 1, "FINISHED", 2);
        cards.sort(Comparator.comparing((LeagueCard c) -> stateOrder.getOrDefault(c.state(), 3)).thenComparing(c -> c.name().toLowerCase(Locale.ROOT)));
        return cards;
    }

    /** seasonNumber null: the latest season. */
    @Transactional
    public LeagueDetail detail(String email, Long leagueId, Integer seasonNumber) {
        AppUser me = player(email);
        return buildDetail(memberLeague(me, leagueId), me.getId(), seasonNumber);
    }

    /** meId null: seen from outside the league (the admin) - nobody is "me", and there are no owner controls. */
    private LeagueDetail buildDetail(League league, Long meId, Integer seasonNumber) {
        List<LeagueSeason> seasons = seasonRepository.findByLeague_IdOrderByNumberAsc(league.getId());
        seasons.forEach(this::ensureScheduled);
        LeagueSeason chosen = seasons.stream()
                .filter(x -> seasonNumber == null || x.getNumber() == seasonNumber)
                .reduce((first, second) -> second)
                .orElseThrow(() -> new ResourceNotFoundException(RequestLang.t("Fant ikke sesongen.", "Season not found.")));
        SeasonView view = seasonView(league, chosen, meId);

        List<LeagueMember> members = memberRepository.findByLeague_IdOrderByJoinedAtAscIdAsc(league.getId());
        Map<Long, String> names = names(members.stream().map(LeagueMember::getUserId).collect(Collectors.toSet()));
        SeasonView current = chosen.equals(seasons.get(seasons.size() - 1)) ? view : seasonView(league, seasons.get(seasons.size() - 1), meId);
        Set<Long> inCurrent = current.participants().stream().map(PlayerRef::id).collect(Collectors.toSet());
        List<MemberView> memberViews = members.stream().filter(LeagueMember::isActive)
                .map(m -> new MemberView(new PlayerRef(m.getUserId(), names.get(m.getUserId()), m.getUserId().equals(meId), false, false),
                        m.getUserId().equals(league.getOwnerUserId()), inCurrent.contains(m.getUserId())))
                .collect(Collectors.toList());

        LeagueSeason latest = seasons.get(seasons.size() - 1);
        String latestState = stateOf(latest);
        boolean owner = league.getOwnerUserId().equals(meId);
        boolean canNew = owner && ("FINISHED".equals(latestState) || "UPCOMING".equals(latestState)
                || (latest.getFormat() == LeagueFormat.POINTS && latest.getEndDate() == null));

        List<SeasonSummary> summaries = seasons.stream()
                .map(x -> new SeasonSummary(x.getNumber(), x.getFormat(), x.getStartDate(), x.getEndDate(), stateOf(x)))
                .collect(Collectors.toList());
        return new LeagueDetail(league.getId(), league.getName(), league.getCode(), owner, canNew, memberViews,
                summaries, view, meId == null ? List.of() : rivalries(league, seasons, chosen, view, meId));
    }

    // ---------- the admin's view ----------

    /** Every league at a glance: totals, and one row per league with how much its players are playing. */
    @Transactional
    public AdminLeagueOverview adminOverview() {
        LocalDate today = today();
        Instant monthAgo = today.minusDays(30).atStartOfDay(clock.getZone()).toInstant();
        LocalDate weekFrom = today.minusDays(7);
        LocalDate weekTo = today.minusDays(1);

        List<League> leagues = leagueRepository.findAll();
        List<LeagueMember> allMembers = memberRepository.findAll();
        Map<Long, List<LeagueMember>> byLeague = allMembers.stream().collect(Collectors.groupingBy(m -> m.getLeague().getId()));
        Map<Long, String> ownerNames = names(leagues.stream().map(League::getOwnerUserId).collect(Collectors.toSet()));

        List<AdminLeagueRow> rows = new ArrayList<>();
        int active = 0, upcoming = 0, finished = 0, h2h = 0, points = 0, playoffs = 0, createdRecently = 0;
        long playedAll = 0, possibleAll = 0;
        for (League league : leagues) {
            List<LeagueSeason> seasons = seasonRepository.findByLeague_IdOrderByNumberAsc(league.getId());
            if (seasons.isEmpty()) continue;
            LeagueSeason season = seasons.get(seasons.size() - 1);
            ensureScheduled(season);
            String state = stateOf(season);
            switch (state) {
                case "ACTIVE" -> active++;
                case "UPCOMING" -> upcoming++;
                default -> finished++;
            }
            if (season.getFormat() == LeagueFormat.HEAD_TO_HEAD) h2h++; else points++;
            if (season.isPlayoffs()) playoffs++;
            if (league.getCreatedAt().isAfter(monthAgo)) createdRecently++;

            List<LeagueMember> members = byLeague.getOrDefault(league.getId(), List.of());
            List<LeagueMatch> matches = season.getFormat() == LeagueFormat.HEAD_TO_HEAD && season.isScheduled()
                    ? matchRepository.findBySeason_IdOrderByQuizDateAscIdAsc(season.getId()) : List.of();
            List<Long> participants = participants(season, members, matches, today);

            // How much of the last 7 finished days the season's players actually played (inside the season)
            LocalDate from = season.getStartDate().isAfter(weekFrom) ? season.getStartDate() : weekFrom;
            LocalDate to = season.getEndDate() != null && season.getEndDate().isBefore(weekTo) ? season.getEndDate() : weekTo;
            int played = 0, possible = 0;
            if (!participants.isEmpty() && !from.isAfter(to)) {
                possible = participants.size() * (int) (to.toEpochDay() - from.toEpochDay() + 1);
                played = dayScoreRepository.findByUserIdInAndQuizDateBetween(participants, from, to).size();
            }
            playedAll += played;
            possibleAll += possible;

            rows.add(new AdminLeagueRow(league.getId(), league.getName(), league.getCode(), ownerNames.get(league.getOwnerUserId()),
                    (int) members.stream().filter(LeagueMember::isActive).count(), season.getFormat(), season.getNumber(), state,
                    season.getStartDate(), season.getEndDate(), season.isPlayoffs(), league.getCreatedAt(), participants.size(),
                    played, possible));
        }
        rows.sort(Comparator.comparing(AdminLeagueRow::createdAt).reversed());

        List<LeagueMember> activeMembers = allMembers.stream().filter(LeagueMember::isActive).collect(Collectors.toList());
        int players = (int) activeMembers.stream().map(LeagueMember::getUserId).distinct().count();
        int joinedRecently = (int) activeMembers.stream().filter(m -> m.getJoinedAt().isAfter(monthAgo)).count();
        double avgMembers = rows.isEmpty() ? 0 : Math.round(activeMembers.size() * 10.0 / rows.size()) / 10.0;
        Integer playedPercent = possibleAll == 0 ? null : (int) Math.round(100.0 * playedAll / possibleAll);
        return new AdminLeagueOverview(rows.size(), active, upcoming, finished, activeMembers.size(), players, avgMembers,
                h2h, points, playoffs, createdRecently, joinedRecently, playedPercent, rows);
    }

    @Transactional
    public LeagueDetail adminDetail(Long leagueId, Integer seasonNumber) {
        League league = leagueRepository.findById(leagueId)
                .orElseThrow(() -> new ResourceNotFoundException(RequestLang.t("Fant ikke ligaen.", "League not found.")));
        return buildDetail(league, null, seasonNumber);
    }

    /** The admin removes a league (an offensive name, say) - for everyone, with all its seasons. */
    @Transactional
    public void adminDelete(Long leagueId) {
        deleteLeague(leagueRepository.findById(leagueId)
                .orElseThrow(() -> new ResourceNotFoundException(RequestLang.t("Fant ikke ligaen.", "League not found."))));
    }

    // ---------- the season, worked out ----------

    SeasonView seasonView(League league, LeagueSeason season, Long meId) {
        LocalDate today = today();
        List<LeagueMember> members = memberRepository.findByLeague_IdOrderByJoinedAtAscIdAsc(league.getId());
        List<LeagueMatch> matches = season.getFormat() == LeagueFormat.HEAD_TO_HEAD && season.isScheduled()
                ? matchRepository.findBySeason_IdOrderByQuizDateAscIdAsc(season.getId()) : List.of();
        List<Long> participants = participants(season, members, matches, today);

        LeagueStandings.Input in = new LeagueStandings.Input();
        in.season = season;
        in.participants = participants;
        in.meId = meId;
        in.today = today;
        in.matches = matches;
        in.expertId = expertConfig.getExpertUserId() > 0 ? expertConfig.getExpertUserId() : null;
        in.names = names(new HashSet<>(participants));
        Set<Long> active = members.stream().filter(LeagueMember::isActive).map(LeagueMember::getUserId).collect(Collectors.toSet());
        in.left = participants.stream().filter(p -> !active.contains(p)).collect(Collectors.toSet());

        boolean started = !today.isBefore(season.getStartDate());
        if (started && !participants.isEmpty()) {
            LocalDate last = season.getEndDate() != null && season.getEndDate().isBefore(today) ? season.getEndDate() : today;
            Set<Long> who = new HashSet<>(participants);
            if (in.expertId != null) who.add(in.expertId);
            Map<Long, Map<LocalDate, Integer>> scores = new HashMap<>();
            dayScoreRepository.findByUserIdInAndQuizDateBetween(who, season.getStartDate(), last)
                    .forEach(r -> scores.computeIfAbsent(r.getUserId(), k -> new HashMap<>()).put(r.getQuizDate(), r.getScore()));
            in.scores = scores;
            in.pending = attemptRepository.findByStatusAndSet_QuizDateBetween(DailyQuizAttemptStatus.SUBMITTED, season.getStartDate(), last).stream()
                    .filter(a -> who.contains(a.getUser().getId()) && LeagueScoreService.submittedOnItsDay(a))
                    .map(a -> LeagueStandings.pendingKey(a.getUser().getId(), a.getSet().getQuizDate()))
                    .collect(Collectors.toSet());
        }
        if (season.getFormat() == LeagueFormat.HEAD_TO_HEAD && !season.isScheduled() && started) {
            in.notice = "NEEDS_PLAYERS";
        } else if (season.getFormat() == LeagueFormat.HEAD_TO_HEAD && season.isScheduled() && active.contains(meId)
                && !participants.contains(meId) && !"FINISHED".equals(stateOf(season))) {
            in.notice = "NOT_IN_SEASON";
        }
        return LeagueStandings.compute(in);
    }

    /**
     * Who plays in a season. Head-to-head: once drawn, exactly the players in the matchups (a member who left
     * stays in, marked as left); before that, the current members. Points: every member who was in the league at
     * some point during the season.
     */
    List<Long> participants(LeagueSeason season, List<LeagueMember> members, List<LeagueMatch> matches, LocalDate today) {
        ZoneId zone = clock.getZone();
        if (season.getFormat() == LeagueFormat.HEAD_TO_HEAD) {
            if (!season.isScheduled()) {
                return members.stream().filter(LeagueMember::isActive).map(LeagueMember::getUserId).collect(Collectors.toList());
            }
            LinkedHashSet<Long> inMatches = new LinkedHashSet<>();
            matches.forEach(m -> { inMatches.add(m.getUserA()); if (m.getUserB() != null) inMatches.add(m.getUserB()); });
            List<Long> ordered = members.stream().map(LeagueMember::getUserId).filter(inMatches::contains).collect(Collectors.toList());
            inMatches.stream().filter(id -> !ordered.contains(id)).forEach(ordered::add); // deleted accounts
            return ordered;
        }
        LocalDate until = season.getEndDate() != null && season.getEndDate().isBefore(today) ? season.getEndDate() : today;
        return members.stream()
                .filter(m -> !LocalDate.ofInstant(m.getJoinedAt(), zone).isAfter(until.isBefore(season.getStartDate()) ? season.getStartDate() : until))
                .filter(m -> m.getLeftAt() == null || !LocalDate.ofInstant(m.getLeftAt(), zone).isBefore(season.getStartDate()))
                .map(LeagueMember::getUserId)
                .collect(Collectors.toList());
    }

    /** My record against each opponent over every head-to-head season of the league, playoffs included. */
    List<RivalryRow> rivalries(League league, List<LeagueSeason> seasons, LeagueSeason chosen, SeasonView chosenView, Long meId) {
        Map<Long, int[]> rec = new LinkedHashMap<>();
        Map<Long, PlayerRef> refs = new HashMap<>();
        for (LeagueSeason season : seasons) {
            if (season.getFormat() != LeagueFormat.HEAD_TO_HEAD || !season.isScheduled()) continue;
            SeasonView v = season.equals(chosen) ? chosenView : seasonView(league, season, meId);
            for (DayView day : v.days()) {
                for (MatchView m : day.matches()) {
                    if (!"FINAL".equals(m.status()) || m.a() == null || m.b() == null) continue;
                    boolean iAmA = m.a().me();
                    boolean iAmB = m.b().me();
                    if (!iAmA && !iAmB) continue;
                    PlayerRef opp = iAmA ? m.b() : m.a();
                    int[] r = rec.computeIfAbsent(opp.id(), k -> new int[3]);
                    refs.put(opp.id(), opp);
                    String mine = iAmA ? "A" : "B";
                    if ("DRAW".equals(m.outcome())) r[1]++;
                    else if (mine.equals(m.outcome())) r[0]++;
                    else r[2]++;
                }
            }
        }
        return rec.entrySet().stream()
                .map(e -> new RivalryRow(refs.get(e.getKey()), e.getValue()[0], e.getValue()[1], e.getValue()[2]))
                .sorted(Comparator.comparingInt((RivalryRow r) -> -(r.wins() + r.draws() + r.losses())))
                .collect(Collectors.toList());
    }

    // ---------- the draw ----------

    /** Catches seasons whose first day has come, so matchups exist even before anyone opens the league. */
    @Scheduled(fixedRate = 60 * 60 * 1000, initialDelay = 60 * 1000)
    @Transactional
    public void scheduleDueSeasons() {
        seasonRepository.findByScheduledFalse().forEach(this::ensureScheduled);
    }

    /**
     * Draws a head-to-head season's matchups on (or after) its first day, from the members at that moment. With
     * fewer than two players nothing can be drawn: the start moves on to today until someone joins. A late draw
     * moves the start to today, so no day is lost to the schedule arriving late. Playoffs need four players.
     */
    synchronized void ensureScheduled(LeagueSeason season) {
        if (season.getFormat() != LeagueFormat.HEAD_TO_HEAD || season.isScheduled()) return;
        LocalDate today = today();
        if (today.isBefore(season.getStartDate())) return;
        List<Long> players = memberRepository.findByLeague_IdOrderByJoinedAtAscIdAsc(season.getLeague().getId()).stream()
                .filter(LeagueMember::isActive).map(LeagueMember::getUserId).collect(Collectors.toList());
        if (season.getStartDate().isBefore(today)) {
            season.setStartDate(today);
            season.recalculateEndDate();
        }
        if (players.size() < 2) {
            seasonRepository.save(season);
            return;
        }
        if (season.isPlayoffs() && players.size() < LeagueSeason.PLAYOFF_SIZE) {
            season.setPlayoffs(false);
            season.recalculateEndDate();
        }
        Long expert = expertConfig.getExpertUserId() > 0 ? expertConfig.getExpertUserId() : null;
        boolean withExpert = expert != null && !players.contains(expert);
        int days = season.getRegularWeeks() * 7;
        List<List<long[]>> draw = LeagueStandings.roundRobin(players, days, withExpert);
        List<LeagueMatch> matches = new ArrayList<>();
        for (int d = 0; d < draw.size(); d++) {
            LocalDate date = season.getStartDate().plusDays(d);
            for (long[] pair : draw.get(d)) {
                matches.add(new LeagueMatch(season, date, pair[0], pair[1] == LeagueStandings.EXPERT ? null : pair[1]));
            }
        }
        matchRepository.saveAll(matches);
        season.setScheduled(true);
        seasonRepository.save(season);
    }

    // ---------- account deletion ----------

    /** A player deleted their account: out of every league (the leagues they ran go to the next member), scores gone. */
    @Transactional
    public void removeUser(Long userId) {
        for (League league : leagueRepository.findByOwnerUserId(userId)) {
            memberRepository.findByLeague_IdAndUserId(league.getId(), userId).ifPresent(m -> {
                m.setLeftAt(Instant.now(clock));
                memberRepository.save(m);
            });
            handOverOrDelete(league);
        }
        memberRepository.deleteByUserId(userId);
        dayScoreRepository.deleteByUserId(userId);
    }

    // ---------- helpers ----------

    private LeagueSeason newSeason(League league, int number, SeasonSettings settings) {
        if (settings == null || settings.format() == null) {
            throw new IllegalArgumentException(RequestLang.t("Velg et format for sesongen.", "Choose a format for the season."));
        }
        LocalDate start = startDate(settings.start());

        LeagueSeason season = new LeagueSeason();
        season.setLeague(league);
        season.setNumber(number);
        season.setFormat(settings.format());
        season.setStartDate(start);
        if (settings.format() == LeagueFormat.HEAD_TO_HEAD) {
            if (settings.weeks() == null || settings.weeks() < 1 || settings.weeks() > MAX_H2H_WEEKS) {
                throw new IllegalArgumentException(RequestLang.t("Velg en lengde på 1-12 uker.", "Choose a length of 1-12 weeks."));
            }
            season.setRegularWeeks(settings.weeks());
            season.setPlayoffs(settings.playoffs());
            season.recalculateEndDate();
        } else {
            if (settings.weeks() != null && (settings.weeks() < 1 || settings.weeks() > MAX_POINTS_WEEKS)) {
                throw new IllegalArgumentException(RequestLang.t("Velg en lengde på 1-52 uker, eller ingen slutt.", "Choose a length of 1-52 weeks, or no end."));
            }
            season.setPlayoffs(false);
            season.setEndDate(settings.weeks() == null ? null : start.plusDays(settings.weeks() * 7L - 1));
        }
        return season;
    }

    /** "TODAY", "TOMORROW" or a date (yyyy-mm-dd) from today up to MAX_DAYS_AHEAD days ahead. */
    LocalDate startDate(String start) {
        if ("TODAY".equals(start)) return today();
        if ("TOMORROW".equals(start)) return today().plusDays(1);
        LocalDate date;
        try {
            date = LocalDate.parse(start == null ? "" : start.trim());
        } catch (java.time.format.DateTimeParseException e) {
            throw new IllegalArgumentException(RequestLang.t("Velg når sesongen starter.", "Choose when the season starts."));
        }
        if (date.isBefore(today())) {
            throw new IllegalArgumentException(RequestLang.t("Startdatoen kan ikke være tilbake i tid.", "The start date can't be in the past."));
        }
        if (date.isAfter(today().plusDays(MAX_DAYS_AHEAD))) {
            throw new IllegalArgumentException(RequestLang.t("Sesongen kan starte senest 90 dager fram i tid.", "The season can start at most 90 days ahead."));
        }
        return date;
    }

    private void removeSeason(LeagueSeason season) {
        matchRepository.deleteBySeason_IdIn(List.of(season.getId()));
        seasonRepository.delete(season);
        seasonRepository.flush();
    }

    private void handOverOrDelete(League league) {
        Optional<LeagueMember> next = memberRepository.findByLeague_IdOrderByJoinedAtAscIdAsc(league.getId()).stream()
                .filter(LeagueMember::isActive).findFirst();
        if (next.isPresent()) {
            league.setOwnerUserId(next.get().getUserId());
            leagueRepository.save(league);
        } else {
            deleteLeague(league);
        }
    }

    private void deleteLeague(League league) {
        List<LeagueSeason> seasons = seasonRepository.findByLeague_IdOrderByNumberAsc(league.getId());
        if (!seasons.isEmpty()) {
            matchRepository.deleteBySeason_IdIn(seasons.stream().map(LeagueSeason::getId).collect(Collectors.toList()));
        }
        seasonRepository.deleteAll(seasons);
        memberRepository.deleteByLeague_Id(league.getId());
        leagueRepository.delete(league);
    }

    String stateOf(LeagueSeason season) {
        LocalDate today = today();
        if (today.isBefore(season.getStartDate())) return "UPCOMING";
        if (season.getEndDate() != null && today.isAfter(season.getEndDate())) return "FINISHED";
        return "ACTIVE";
    }

    private LeagueSeason latestSeason(League league) {
        List<LeagueSeason> seasons = seasonRepository.findByLeague_IdOrderByNumberAsc(league.getId());
        return seasons.get(seasons.size() - 1);
    }

    private Map<Long, String> names(Set<Long> ids) {
        Map<Long, String> out = new HashMap<>();
        appUserRepository.findAllById(ids).forEach(u -> out.put(u.getId(), u.getName()));
        return out;
    }

    private AppUser player(String email) {
        return appUserRepository.findByEmail(email).orElseThrow(() ->
                new IllegalArgumentException(RequestLang.t("Ligaer er for spillerkontoer.", "Leagues are for player accounts.")));
    }

    private League byCode(String code) {
        String c = code == null ? "" : code.trim().toUpperCase(Locale.ROOT);
        return leagueRepository.findByCode(c).orElseThrow(() ->
                new ResourceNotFoundException(RequestLang.t("Fant ingen liga med den koden.", "No league has that code.")));
    }

    /** The league, if I'm in it - otherwise it doesn't exist as far as I can tell. */
    private League memberLeague(AppUser me, Long leagueId) {
        return memberRepository.findByLeague_IdAndUserId(leagueId, me.getId())
                .filter(LeagueMember::isActive)
                .map(LeagueMember::getLeague)
                .orElseThrow(() -> new ResourceNotFoundException(RequestLang.t("Fant ikke ligaen.", "League not found.")));
    }

    private League ownedLeague(AppUser me, Long leagueId) {
        League league = memberLeague(me, leagueId);
        if (!league.getOwnerUserId().equals(me.getId())) {
            throw new IllegalStateException(RequestLang.t("Bare den som styrer ligaen kan gjøre dette.", "Only the league's owner can do this."));
        }
        return league;
    }

    private String newCode() {
        for (int attempt = 0; attempt < 50; attempt++) {
            StringBuilder sb = new StringBuilder(CODE_LENGTH);
            for (int i = 0; i < CODE_LENGTH; i++) sb.append(CODE_ALPHABET.charAt(RANDOM.nextInt(CODE_ALPHABET.length())));
            if (!leagueRepository.existsByCode(sb.toString())) return sb.toString();
        }
        throw new IllegalStateException("Could not find a free league code");
    }
}
