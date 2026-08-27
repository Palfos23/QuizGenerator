package com.quizapp.service;

import com.quizapp.dto.AdminStatisticsDto;
import com.quizapp.model.AppUser;
import com.quizapp.model.Athlete;
import com.quizapp.model.Grid;
import com.quizapp.model.GridAttempt;
import com.quizapp.model.GridEntry;
import com.quizapp.repository.AppUserRepository;
import com.quizapp.repository.AthleteRepository;
import com.quizapp.repository.GridAttemptRepository;
import com.quizapp.repository.GridRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class StatisticsServiceTest {

    @Autowired
    private StatisticsService statisticsService;
    @Autowired
    private AppUserRepository appUserRepository;
    @Autowired
    private AthleteRepository athleteRepository;
    @Autowired
    private GridRepository gridRepository;
    @Autowired
    private GridAttemptRepository gridAttemptRepository;

    // A category name unique to this test run so assertions aren't disturbed by
    // grids/athletes other test classes seed into the shared H2 instance.
    private String uniqueCategory() {
        return "StatCat-" + System.nanoTime();
    }

    @Test
    void reportsHeadlineTotalsAndGameModeBreakdown() {
        long usersBefore = statisticsService.build().getTotalUsers();
        appUserRepository.save(newUser());

        AdminStatisticsDto stats = statisticsService.build();

        assertThat(stats.getTotalUsers()).isEqualTo(usersBefore + 1);
        assertThat(stats.getBoardsByGameMode())
                .extracting(AdminStatisticsDto.CountEntry::getLabel)
                .contains("Weekly grids", "Starting XI", "Tension", "501", "Imposter", "Bullseye");
        assertThat(stats.getUsersByMonth()).hasSize(12);
        assertThat(stats.getUsersByMonth().get(11).getCount()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void groupsSubjectsByCategory() {
        String category = uniqueCategory();
        athleteRepository.save(newAthlete(category));
        athleteRepository.save(newAthlete(category));

        AdminStatisticsDto stats = statisticsService.build();

        assertThat(stats.getSubjectsByCategory())
                .filteredOn(e -> e.getLabel().equals(category))
                .singleElement()
                .satisfies(e -> assertThat(e.getCount()).isEqualTo(2));
    }

    @Test
    void computesThisWeeksGridScoreSpread() {
        String category = uniqueCategory();
        Grid grid = saveActiveGridWithEntries(category, 5);
        List<Long> entryIds = grid.getEntries().stream().map(GridEntry::getId).toList();

        // Three completed attempts scoring 1, 3 and 5 correct answers.
        saveCompletedAttempt(grid, entryIds.subList(0, 1), Set.of());
        saveCompletedAttempt(grid, entryIds.subList(0, 3), Set.of());
        saveCompletedAttempt(grid, entryIds.subList(0, 5), Set.of());
        // A fourth attempt that only "found" tiles during overtime - contributes a
        // score of 0 and must drag the average and the low mark down.
        saveCompletedAttempt(grid, entryIds.subList(0, 2), Set.copyOf(entryIds.subList(0, 2)));

        AdminStatisticsDto.WeeklyGridStat stat = statisticsService.build().getWeeklyGrids().stream()
                .filter(g -> g.getGridId().equals(grid.getId()))
                .findFirst()
                .orElseThrow();

        assertThat(stat.getPlayers()).isEqualTo(4);
        assertThat(stat.getEntryCount()).isEqualTo(5);
        assertThat(stat.getLowestScore()).isEqualTo(0);
        assertThat(stat.getHighestScore()).isEqualTo(5);
        assertThat(stat.getAverageScore()).isEqualTo((1 + 3 + 5 + 0) / 4.0);
    }

    private AppUser newUser() {
        AppUser u = new AppUser();
        u.setEmail("stat-user-" + System.nanoTime() + "@example.com");
        u.setName("Stat User");
        return u;
    }

    private Athlete newAthlete(String category) {
        Athlete a = new Athlete();
        a.setName("Subject " + System.nanoTime());
        a.setSport(category);
        return athleteRepository.save(a);
    }

    private Grid saveActiveGridWithEntries(String category, int entryCount) {
        Grid g = new Grid();
        g.setTitle("Stat Grid " + System.nanoTime());
        g.setSport(category);
        g.setWeekStartDate(LocalDate.now());
        g.setMaxStrikes(3);

        Set<GridEntry> entries = new HashSet<>();
        for (int i = 0; i < entryCount; i++) {
            GridEntry e = new GridEntry();
            e.setAthlete(newAthlete(category));
            e.setOrderIndex(i);
            entries.add(e);
        }
        g.setEntries(entries);
        return gridRepository.save(g);
    }

    private void saveCompletedAttempt(Grid grid, List<Long> solvedEntryIds, Set<Long> overtimeEntryIds) {
        AppUser user = appUserRepository.save(newUser());
        GridAttempt a = new GridAttempt();
        a.setGrid(grid);
        a.setUser(user);
        a.setCompleted(true);
        a.setSolvedEntryIds(new HashSet<>(solvedEntryIds));
        a.setOvertimeSolvedEntryIds(new HashSet<>(overtimeEntryIds));
        gridAttemptRepository.save(a);
    }
}
