package com.quizapp.service;

import com.quizapp.dto.BdayNightDto.*;
import com.quizapp.model.*;
import com.quizapp.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * The birthday night's combined scoreboard. A guest's total is the sum of four things:
 * the quiz score, Mario Kart points (from where their best lap ranks among everyone's), the music quiz score of their
 * team, and the bonus points from their one spin of the wheel. Everything is worked out here from the raw results,
 * so changing a time, a team score or a points table updates every total at once - nothing is stored twice.
 */
@Service
public class BdayStandingsService {

    static final String KEY_MK_OPEN = "mk_open";
    static final String KEY_MK_POINTS = "mk_points";
    static final String KEY_MK_OTHER = "mk_other";
    static final String DEFAULT_MK_POINTS = "10, 8, 6, 5, 4, 3, 2, 1";
    static final int DEFAULT_MK_OTHER = 1;

    private final BdayGuestRepository guestRepository;
    private final BdayMarioKartTimeRepository timeRepository;
    private final BdayTeamRepository teamRepository;
    private final BdayWheelSpinRepository spinRepository;
    private final BdaySettingRepository settingRepository;

    public BdayStandingsService(BdayGuestRepository guestRepository, BdayMarioKartTimeRepository timeRepository,
                                BdayTeamRepository teamRepository, BdayWheelSpinRepository spinRepository,
                                BdaySettingRepository settingRepository) {
        this.guestRepository = guestRepository;
        this.timeRepository = timeRepository;
        this.teamRepository = teamRepository;
        this.spinRepository = spinRepository;
        this.settingRepository = settingRepository;
    }

    // ------------------------------------------------------------------ settings

    @Transactional(readOnly = true)
    public Settings settings() {
        return new Settings(
                "true".equals(read(KEY_MK_OPEN, "false")),
                read(KEY_MK_POINTS, DEFAULT_MK_POINTS),
                parseIntOr(read(KEY_MK_OTHER, String.valueOf(DEFAULT_MK_OTHER)), DEFAULT_MK_OTHER));
    }

    @Transactional
    public Settings updateSettings(Settings update) {
        List<Integer> table = parsePointsTable(update.marioKartPoints()); // validates
        if (update.marioKartOtherPoints() < 0 || update.marioKartOtherPoints() > 1000) {
            throw new IllegalArgumentException("Poeng for alle andre må være mellom 0 og 1000.");
        }
        write(KEY_MK_OPEN, String.valueOf(update.marioKartOpen()));
        write(KEY_MK_POINTS, table.stream().map(String::valueOf).collect(Collectors.joining(", ")));
        write(KEY_MK_OTHER, String.valueOf(update.marioKartOtherPoints()));
        return settings();
    }

    /** "10, 8, 6" (commas, spaces or semicolons) -> [10, 8, 6]. At least one number, none negative. */
    static List<Integer> parsePointsTable(String raw) {
        List<Integer> out = new ArrayList<>();
        for (String part : (raw == null ? "" : raw).split("[,;\\s]+")) {
            if (part.isBlank()) continue;
            int value;
            try {
                value = Integer.parseInt(part.trim());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Poengene per plass må være tall, atskilt med komma - f.eks. 10, 8, 6, 5.");
            }
            if (value < 0 || value > 1000) {
                throw new IllegalArgumentException("Poeng per plass må være mellom 0 og 1000.");
            }
            out.add(value);
        }
        if (out.isEmpty()) throw new IllegalArgumentException("Skriv inn poeng for minst én plass.");
        if (out.size() > 30) throw new IllegalArgumentException("Maks 30 plasser.");
        return out;
    }

    /** Points for a rank (1 = fastest): the table entry for it, or the "everyone else" points past the end of the table. */
    static int pointsForRank(int rank, List<Integer> table, int otherPoints) {
        return rank >= 1 && rank <= table.size() ? table.get(rank - 1) : otherPoints;
    }

    // ------------------------------------------------------------------ Mario Kart ranking

    /** Everyone with a lap time, fastest first. Equal times share a rank (and the points that rank is worth). */
    @Transactional(readOnly = true)
    public List<LapEntry> lapRanking() {
        Settings settings = settings();
        List<Integer> table = parsePointsTable(settings.marioKartPoints());
        List<BdayMarioKartTime> times = timeRepository.findAll().stream()
                .sorted(Comparator.comparingInt(BdayMarioKartTime::getTimeMs).thenComparing(BdayMarioKartTime::getEnteredAt))
                .collect(Collectors.toList());
        List<LapEntry> out = new ArrayList<>();
        for (BdayMarioKartTime t : times) {
            int rank = 1 + (int) times.stream().filter(o -> o.getTimeMs() < t.getTimeMs()).count();
            out.add(new LapEntry(rank, t.getGuest().getId(), t.getGuest().getName(), t.getTimeMs(),
                    pointsForRank(rank, table, settings.marioKartOtherPoints())));
        }
        return out;
    }

    // ------------------------------------------------------------------ the combined board

    @Transactional(readOnly = true)
    public Standings standings() {
        List<LapEntry> laps = lapRanking();
        Map<Long, LapEntry> lapByGuest = laps.stream().collect(Collectors.toMap(LapEntry::guestId, l -> l));
        Map<Long, Integer> bonusByGuest = spinRepository.findAll().stream()
                .collect(Collectors.toMap(s -> s.getGuest().getId(), BdayWheelSpin::getBonusPoints));

        List<PlayerStanding> unsorted = new ArrayList<>();
        for (BdayGuest g : guestRepository.findAll()) {
            boolean quizDone = g.getStatus() == BdayGuest.Status.DONE;
            int quiz = quizDone ? g.getScore() : 0;
            LapEntry lap = lapByGuest.get(g.getId());
            int mk = lap == null ? 0 : lap.points();
            int music = g.getTeam() == null ? 0 : g.getTeam().getMusicScore();
            int bonus = bonusByGuest.getOrDefault(g.getId(), 0);
            unsorted.add(new PlayerStanding(g.getId(), g.getName(), g.getTeam() == null ? null : g.getTeam().getName(), 0,
                    quizDone, quiz, lap == null ? null : lap.timeMs(), lap == null ? null : lap.rank(), mk, music, bonus,
                    quiz + mk + music + bonus));
        }
        unsorted.sort(Comparator.comparingInt(PlayerStanding::total).reversed()
                .thenComparing(p -> p.name().toLowerCase(Locale.ROOT)));

        // equal totals share a rank
        List<PlayerStanding> players = new ArrayList<>();
        for (int i = 0; i < unsorted.size(); i++) {
            PlayerStanding p = unsorted.get(i);
            int rank = i > 0 && unsorted.get(i - 1).total() == p.total() ? players.get(i - 1).rank() : i + 1;
            players.add(new PlayerStanding(p.guestId(), p.name(), p.teamName(), rank, p.quizDone(), p.quizScore(),
                    p.marioKartTimeMs(), p.marioKartRank(), p.marioKartPoints(), p.musicPoints(), p.bonusPoints(), p.total()));
        }

        Map<Long, Long> memberCounts = guestRepository.findAll().stream()
                .filter(g -> g.getTeam() != null)
                .collect(Collectors.groupingBy(g -> g.getTeam().getId(), Collectors.counting()));
        List<TeamStanding> teams = teamRepository.findAllByOrderByNameAsc().stream()
                .map(t -> new TeamStanding(t.getId(), t.getName(), t.getMusicScore(), memberCounts.getOrDefault(t.getId(), 0L).intValue()))
                .sorted(Comparator.comparingInt(TeamStanding::musicScore).reversed().thenComparing(TeamStanding::name))
                .collect(Collectors.toList());

        return new Standings(players, teams, laps, settings().marioKartOpen());
    }

    // ------------------------------------------------------------------ helpers

    private String read(String key, String fallback) {
        return settingRepository.findById(key).map(BdaySetting::getSettingValue).orElse(fallback);
    }

    private void write(String key, String value) {
        BdaySetting s = settingRepository.findById(key).orElseGet(() -> new BdaySetting(key, value));
        s.setSettingValue(value);
        settingRepository.save(s);
    }

    private static int parseIntOr(String raw, int fallback) {
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
