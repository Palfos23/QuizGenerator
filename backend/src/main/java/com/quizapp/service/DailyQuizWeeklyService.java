package com.quizapp.service;

import com.quizapp.dto.DailyQuizWeeklyDto;
import com.quizapp.model.AppUser;
import com.quizapp.model.DailyQuizAttempt;
import com.quizapp.model.DailyQuizAttemptStatus;
import com.quizapp.model.DailyQuizResult;
import com.quizapp.repository.AppUserRepository;
import com.quizapp.repository.DailyQuizAttemptRepository;
import com.quizapp.repository.DailyQuizResultRepository;
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
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The weekly winner of the daily quiz: a week runs Monday to Sunday, a player's weekly total is the sum
 * of their graded daily scores in it, and the highest total wins (a tie shares the win).
 *
 * Worked out entirely from the compact per-player-per-day result records (DailyQuizResult), never from
 * the quizzes themselves - those are deleted after 7 days, which is before some weeks' early days
 * could otherwise be counted. Players who've opted out of the leaderboard are left out, exactly as on
 * the daily scoreboard (apart from seeing their own row).
 */
@Service
public class DailyQuizWeeklyService {

    // How many finished weeks' winners to list below the current standings.
    static final int PAST_WEEKS = 4;

    private final DailyQuizResultRepository resultRepository;
    private final DailyQuizAttemptRepository attemptRepository;
    private final AppUserRepository appUserRepository;
    private final ExpertConfig expertConfig;

    public DailyQuizWeeklyService(DailyQuizResultRepository resultRepository,
                                   DailyQuizAttemptRepository attemptRepository,
                                   AppUserRepository appUserRepository,
                                   ExpertConfig expertConfig) {
        this.expertConfig = expertConfig;
        this.resultRepository = resultRepository;
        this.attemptRepository = attemptRepository;
        this.appUserRepository = appUserRepository;
    }

    @Transactional(readOnly = true)
    public DailyQuizWeeklyDto getWeekly(String requestingEmail) {
        return getWeekly(requestingEmail, LocalDate.now());
    }

    // `today` is a parameter so the week boundaries can be tested without depending on the real date.
    @Transactional(readOnly = true)
    DailyQuizWeeklyDto getWeekly(String requestingEmail, LocalDate today) {
        // An admin account isn't in the player table - then there simply is no "you" row.
        Long myId = appUserRepository.findByEmail(requestingEmail).map(AppUser::getId).orElse(null);

        LocalDate thisMonday = weekStart(today);
        LocalDate from = thisMonday.minusWeeks(PAST_WEEKS);
        LocalDate to = thisMonday.plusDays(6);
        List<DailyQuizResult> everyone = resultRepository.findByQuizDateBetween(from, to);
        // The expert is the benchmark, not a competitor: out of the standings and the winners, shown on their own.
        List<DailyQuizResult> all = everyone.stream().filter(r -> !expertConfig.isExpert(r.getUserId())).collect(Collectors.toList());
        List<DailyQuizResult> expertThisWeek = inWeek(everyone, thisMonday).stream()
                .filter(r -> expertConfig.isExpert(r.getUserId())).collect(Collectors.toList());

        // Days that still have an attempt waiting on an admin - a week containing one isn't settled.
        Set<LocalDate> daysStillBeingReviewed = attemptRepository
                .findByStatusAndSet_QuizDateBetween(DailyQuizAttemptStatus.SUBMITTED, from, to).stream()
                .map(a -> a.getSet().getQuizDate())
                .collect(Collectors.toSet());

        DailyQuizWeeklyDto.WeekDto current = new DailyQuizWeeklyDto.WeekDto(
                thisMonday, to, standings(inWeek(all, thisMonday), myId));

        List<DailyQuizWeeklyDto.PastWeekDto> past = new ArrayList<>();
        for (int k = 1; k <= PAST_WEEKS; k++) {
            LocalDate start = thisMonday.minusWeeks(k);
            LocalDate end = start.plusDays(6);
            // Winners are named from opted-in results only, whoever is asking.
            List<DailyQuizWeeklyDto.StandingDto> ranked = standings(inWeek(all, start), null);
            if (ranked.isEmpty()) continue;
            int top = ranked.get(0).getTotal();
            List<String> winners = ranked.stream().filter(s -> s.getTotal() == top)
                    .map(DailyQuizWeeklyDto.StandingDto::getPlayerName).collect(Collectors.toList());
            boolean provisional = daysStillBeingReviewed.stream().anyMatch(d -> !d.isBefore(start) && !d.isAfter(end));
            past.add(new DailyQuizWeeklyDto.PastWeekDto(start, end, winners, top, provisional));
        }
        DailyQuizWeeklyDto dto = new DailyQuizWeeklyDto(current, past);
        if (!expertThisWeek.isEmpty()) {
            String name = expertThisWeek.stream().max(Comparator.comparing(DailyQuizResult::getQuizDate)).get().getPlayerName();
            dto.setExpert(new DailyQuizWeeklyDto.ExpertWeek(name,
                    expertThisWeek.stream().mapToInt(DailyQuizResult::getScore).sum(), expertThisWeek.size()));
        }
        return dto;
    }

    static LocalDate weekStart(LocalDate date) {
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    private static List<DailyQuizResult> inWeek(List<DailyQuizResult> all, LocalDate monday) {
        LocalDate sunday = monday.plusDays(6);
        return all.stream().filter(r -> !r.getQuizDate().isBefore(monday) && !r.getQuizDate().isAfter(sunday)).collect(Collectors.toList());
    }

    /** Ranked best first. A result counts if its owner is on the leaderboard - or is the one asking about themselves. */
    private static List<DailyQuizWeeklyDto.StandingDto> standings(List<DailyQuizResult> rows, Long myId) {
        Map<Long, List<DailyQuizResult>> byUser = rows.stream()
                .filter(r -> r.isIncludeOnLeaderboard() || Objects.equals(r.getUserId(), myId))
                .collect(Collectors.groupingBy(DailyQuizResult::getUserId));

        return byUser.entrySet().stream().map(e -> {
            List<DailyQuizResult> mine = e.getValue();
            String name = mine.stream().max(Comparator.comparing(DailyQuizResult::getQuizDate)).get().getPlayerName();
            return new DailyQuizWeeklyDto.StandingDto(
                    name, mine.stream().mapToInt(DailyQuizResult::getScore).sum(), mine.size(), Objects.equals(e.getKey(), myId));
        }).sorted(Comparator.comparingInt(DailyQuizWeeklyDto.StandingDto::getTotal).reversed()
                .thenComparing(DailyQuizWeeklyDto.StandingDto::getPlayerName, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
    }
}
