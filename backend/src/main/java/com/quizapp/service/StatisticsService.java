package com.quizapp.service;

import com.quizapp.dto.AdminStatisticsDto;
import com.quizapp.dto.AdminStatisticsDto.CountEntry;
import com.quizapp.dto.AdminStatisticsDto.WeeklyGridStat;
import com.quizapp.model.Athlete;
import com.quizapp.model.Grid;
import com.quizapp.model.GridAttempt;
import com.quizapp.model.TensionQuestion;
import com.quizapp.repository.AppUserRepository;
import com.quizapp.repository.AthleteRepository;
import com.quizapp.repository.BullseyeQuestionRepository;
import com.quizapp.repository.FiveOhOneCategoryRepository;
import com.quizapp.repository.GridAttemptRepository;
import com.quizapp.repository.GridCategoryRepository;
import com.quizapp.repository.GridRepository;
import com.quizapp.repository.ImposterGridRepository;
import com.quizapp.repository.LineupRepository;
import com.quizapp.repository.TensionQuestionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Assembles the admin Statistics page payload. Read-only and intentionally
 * straightforward - every collection it groups over (grids, athletes, tension
 * questions, users) is small enough to pull in full and tally in memory rather
 * than pushing a dozen aggregate queries into the repositories.
 */
@Service
public class StatisticsService {

    private final AppUserRepository appUserRepository;
    private final AthleteRepository athleteRepository;
    private final GridCategoryRepository gridCategoryRepository;
    private final GridRepository gridRepository;
    private final GridAttemptRepository gridAttemptRepository;
    private final LineupRepository lineupRepository;
    private final ImposterGridRepository imposterGridRepository;
    private final BullseyeQuestionRepository bullseyeQuestionRepository;
    private final FiveOhOneCategoryRepository fiveOhOneCategoryRepository;
    private final TensionQuestionRepository tensionQuestionRepository;

    public StatisticsService(AppUserRepository appUserRepository,
                             AthleteRepository athleteRepository,
                             GridCategoryRepository gridCategoryRepository,
                             GridRepository gridRepository,
                             GridAttemptRepository gridAttemptRepository,
                             LineupRepository lineupRepository,
                             ImposterGridRepository imposterGridRepository,
                             BullseyeQuestionRepository bullseyeQuestionRepository,
                             FiveOhOneCategoryRepository fiveOhOneCategoryRepository,
                             TensionQuestionRepository tensionQuestionRepository) {
        this.appUserRepository = appUserRepository;
        this.athleteRepository = athleteRepository;
        this.gridCategoryRepository = gridCategoryRepository;
        this.gridRepository = gridRepository;
        this.gridAttemptRepository = gridAttemptRepository;
        this.lineupRepository = lineupRepository;
        this.imposterGridRepository = imposterGridRepository;
        this.bullseyeQuestionRepository = bullseyeQuestionRepository;
        this.fiveOhOneCategoryRepository = fiveOhOneCategoryRepository;
        this.tensionQuestionRepository = tensionQuestionRepository;
    }

    @Transactional(readOnly = true)
    public AdminStatisticsDto build() {
        AdminStatisticsDto dto = new AdminStatisticsDto();

        dto.setTotalUsers(appUserRepository.count());
        dto.setTotalSubjects(athleteRepository.count());
        dto.setTotalCategories(gridCategoryRepository.count());

        dto.setUsersByMonth(usersByMonth());
        dto.setBoardsByGameMode(boardsByGameMode());
        dto.setSubjectsByCategory(subjectsByCategory());
        dto.setGridsByCategory(gridsByCategory());
        dto.setTensionQuestionsByCategory(tensionQuestionsByCategory());
        dto.setWeeklyGrids(weeklyGridStats());

        return dto;
    }

    private List<CountEntry> usersByMonth() {
        ZoneId zone = ZoneId.systemDefault();
        YearMonth current = YearMonth.now(zone);
        YearMonth earliest = current.minusMonths(11);

        // Seed the last 12 months at zero so gaps show as gaps, not as missing bars.
        Map<YearMonth, Long> counts = new TreeMap<>();
        for (YearMonth m = earliest; !m.isAfter(current); m = m.plusMonths(1)) {
            counts.put(m, 0L);
        }

        appUserRepository.findAll().forEach(u -> {
            if (u.getCreatedAt() == null) return;
            YearMonth m = YearMonth.from(u.getCreatedAt().atZone(zone));
            if (m.isBefore(earliest)) return; // older sign-ups fall outside the 12-month window
            counts.merge(m, 1L, Long::sum);
        });

        List<CountEntry> out = new ArrayList<>();
        counts.forEach((month, count) -> out.add(new CountEntry(month.toString(), count)));
        return out;
    }

    private List<CountEntry> boardsByGameMode() {
        List<CountEntry> out = new ArrayList<>();
        out.add(new CountEntry("Weekly grids", gridRepository.count()));
        out.add(new CountEntry("Starting XI", lineupRepository.count()));
        out.add(new CountEntry("Tension", tensionQuestionRepository.count()));
        out.add(new CountEntry("501", fiveOhOneCategoryRepository.count()));
        out.add(new CountEntry("Imposter", imposterGridRepository.count()));
        out.add(new CountEntry("Bullseye", bullseyeQuestionRepository.count()));
        return out;
    }

    private List<CountEntry> subjectsByCategory() {
        Map<String, Long> byCategory = athleteRepository.findAll().stream()
                .collect(Collectors.groupingBy(Athlete::getSport, Collectors.counting()));
        return toSortedEntries(byCategory);
    }

    private List<CountEntry> gridsByCategory() {
        Map<String, Long> byCategory = gridRepository.findAllSummaries().stream()
                .collect(Collectors.groupingBy(p -> p.getSport() == null ? "Uncategorized" : p.getSport(),
                        Collectors.counting()));
        return toSortedEntries(byCategory);
    }

    private List<CountEntry> tensionQuestionsByCategory() {
        Map<String, Long> byCategory = tensionQuestionRepository.findAll().stream()
                .collect(Collectors.groupingBy(StatisticsService::tensionCategoryOf, Collectors.counting()));
        return toSortedEntries(byCategory);
    }

    private static String tensionCategoryOf(TensionQuestion q) {
        String c = q.getMainCategory();
        return (c == null || c.isBlank()) ? "Uncategorized" : c;
    }

    private List<WeeklyGridStat> weeklyGridStats() {
        LocalDate today = LocalDate.now();
        List<Grid> activeGrids = gridRepository
                .findByWeekStartDateLessThanEqualOrderByWeekStartDateDesc(today).stream()
                .filter(g -> !today.isAfter(g.getWeekStartDate().plusDays(6)))
                .sorted(Comparator.comparing(Grid::getSport, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(Grid::getTitle))
                .collect(Collectors.toList());

        List<WeeklyGridStat> out = new ArrayList<>();
        for (Grid grid : activeGrids) {
            int entryCount = grid.getEntries().size();

            List<Integer> scores = gridAttemptRepository.findByGrid_Id(grid.getId()).stream()
                    .filter(GridAttempt::isCompleted)
                    .map(StatisticsService::scoreOf)
                    .collect(Collectors.toList());

            int players = scores.size();
            double average = players == 0 ? 0
                    : scores.stream().mapToInt(Integer::intValue).average().orElse(0);
            int lowest = scores.stream().mapToInt(Integer::intValue).min().orElse(0);
            int highest = scores.stream().mapToInt(Integer::intValue).max().orElse(0);

            out.add(new WeeklyGridStat(grid.getId(), grid.getTitle(), grid.getSport(), grid.getWeekStartDate(),
                    entryCount, players, average, lowest, highest));
        }
        return out;
    }

    // Correct answers found within the player's original lives - overtime solves
    // don't count, matching GridPlayService.getScoreboard.
    private static int scoreOf(GridAttempt a) {
        return a.getSolvedEntryIds().size() - a.getOvertimeSolvedEntryIds().size();
    }

    private static List<CountEntry> toSortedEntries(Map<String, Long> counts) {
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed()
                        .thenComparing(Map.Entry.comparingByKey()))
                .map(e -> new CountEntry(e.getKey(), e.getValue()))
                .collect(Collectors.toCollection(ArrayList::new));
    }
}
