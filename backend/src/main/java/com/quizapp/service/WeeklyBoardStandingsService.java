package com.quizapp.service;

import com.quizapp.dto.DailyQuizWeeklyDto;
import com.quizapp.model.GridAttempt;
import com.quizapp.model.LineupAttempt;
import com.quizapp.repository.GridAttemptRepository;
import com.quizapp.repository.LineupAttemptRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Weekly standings for the two weekly games (Weekly Grid and Starting XI), shaped like the daily
 * quiz's (DailyQuizWeeklyDto) so the front end shows them in the same panel: a week runs Monday to
 * Sunday, counted by when each board went live, a player's weekly total is what they found across
 * that week's boards, and the highest total wins (a tie shares it).
 *
 * Like each board's own leaderboard, only completed attempts count, and players who've opted out are
 * left out (apart from seeing their own row). For grids, overtime solves don't count toward the score.
 * `daysPlayed` in the shared DTO is the number of boards the player completed that week.
 */
@Service
public class WeeklyBoardStandingsService {

    // How many finished weeks' winners to list below the current standings.
    static final int PAST_WEEKS = 4;

    private final GridAttemptRepository gridAttemptRepository;
    private final LineupAttemptRepository lineupAttemptRepository;

    public WeeklyBoardStandingsService(GridAttemptRepository gridAttemptRepository,
                                        LineupAttemptRepository lineupAttemptRepository) {
        this.gridAttemptRepository = gridAttemptRepository;
        this.lineupAttemptRepository = lineupAttemptRepository;
    }

    /** One completed attempt, reduced to what the standings need. */
    record Row(Long userId, String userName, String userEmail, LocalDate weekStart, int score, boolean includeOnLeaderboard) {}

    @Transactional(readOnly = true)
    public DailyQuizWeeklyDto getGridWeekly(String requestingEmail) {
        return getGridWeekly(requestingEmail, LocalDate.now());
    }

    @Transactional(readOnly = true)
    DailyQuizWeeklyDto getGridWeekly(String requestingEmail, LocalDate today) {
        LocalDate thisMonday = weekStart(today);
        List<Row> rows = gridAttemptRepository
                .findByGrid_WeekStartDateBetween(thisMonday.minusWeeks(PAST_WEEKS), thisMonday.plusDays(6)).stream()
                .filter(GridAttempt::isCompleted)
                .map(a -> new Row(a.getUser().getId(), a.getUser().getName(), a.getUser().getEmail(),
                        weekStart(a.getGrid().getWeekStartDate()),
                        a.getSolvedEntryIds().size() - a.getOvertimeSolvedEntryIds().size(), a.isIncludeOnLeaderboard()))
                .collect(Collectors.toList());
        return build(rows, requestingEmail, thisMonday);
    }

    @Transactional(readOnly = true)
    public DailyQuizWeeklyDto getLineupWeekly(String requestingEmail) {
        return getLineupWeekly(requestingEmail, LocalDate.now());
    }

    @Transactional(readOnly = true)
    DailyQuizWeeklyDto getLineupWeekly(String requestingEmail, LocalDate today) {
        LocalDate thisMonday = weekStart(today);
        List<Row> rows = lineupAttemptRepository
                .findByLineup_WeekStartDateBetween(thisMonday.minusWeeks(PAST_WEEKS), thisMonday.plusDays(6)).stream()
                .filter(LineupAttempt::isCompleted)
                .map(a -> new Row(a.getUser().getId(), a.getUser().getName(), a.getUser().getEmail(),
                        weekStart(a.getLineup().getWeekStartDate()),
                        a.getSolvedEntryIds().size(), a.isIncludeOnLeaderboard()))
                .collect(Collectors.toList());
        return build(rows, requestingEmail, thisMonday);
    }

    static DailyQuizWeeklyDto build(List<Row> all, String requestingEmail, LocalDate thisMonday) {
        LocalDate thisSunday = thisMonday.plusDays(6);
        DailyQuizWeeklyDto.WeekDto current = new DailyQuizWeeklyDto.WeekDto(
                thisMonday, thisSunday, standings(inWeek(all, thisMonday), requestingEmail));

        List<DailyQuizWeeklyDto.PastWeekDto> past = new ArrayList<>();
        for (int k = 1; k <= PAST_WEEKS; k++) {
            LocalDate start = thisMonday.minusWeeks(k);
            // Winners are named from opted-in results only, whoever is asking.
            List<DailyQuizWeeklyDto.StandingDto> ranked = standings(inWeek(all, start), null);
            if (ranked.isEmpty()) continue;
            int top = ranked.get(0).getTotal();
            List<String> winners = ranked.stream().filter(s -> s.getTotal() == top)
                    .map(DailyQuizWeeklyDto.StandingDto::getPlayerName).collect(Collectors.toList());
            past.add(new DailyQuizWeeklyDto.PastWeekDto(start, start.plusDays(6), winners, top, false));
        }
        return new DailyQuizWeeklyDto(current, past);
    }

    static LocalDate weekStart(LocalDate date) {
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    private static List<Row> inWeek(List<Row> all, LocalDate monday) {
        return all.stream().filter(r -> r.weekStart().equals(monday)).collect(Collectors.toList());
    }

    /** Ranked best first. A row counts if its owner is on the leaderboard - or is the one asking about themselves. */
    private static List<DailyQuizWeeklyDto.StandingDto> standings(List<Row> rows, String requestingEmail) {
        Map<Long, List<Row>> byUser = rows.stream()
                .filter(r -> r.includeOnLeaderboard() || (requestingEmail != null && requestingEmail.equals(r.userEmail())))
                .collect(Collectors.groupingBy(Row::userId));

        return byUser.values().stream().map(mine -> {
            Row first = mine.get(0);
            return new DailyQuizWeeklyDto.StandingDto(first.userName(), mine.stream().mapToInt(Row::score).sum(), mine.size(),
                    requestingEmail != null && Objects.equals(requestingEmail, first.userEmail()));
        }).sorted(Comparator.comparingInt(DailyQuizWeeklyDto.StandingDto::getTotal).reversed()
                .thenComparing(DailyQuizWeeklyDto.StandingDto::getPlayerName, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
    }
}
