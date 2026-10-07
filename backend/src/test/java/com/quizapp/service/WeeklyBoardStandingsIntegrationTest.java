package com.quizapp.service;

import com.quizapp.dto.DailyQuizWeeklyDto;
import com.quizapp.model.AppUser;
import com.quizapp.model.Athlete;
import com.quizapp.model.Grid;
import com.quizapp.model.GridAttempt;
import com.quizapp.model.GridEntry;
import com.quizapp.model.Lineup;
import com.quizapp.model.LineupAttempt;
import com.quizapp.model.LineupEntry;
import com.quizapp.repository.AppUserRepository;
import com.quizapp.repository.AthleteRepository;
import com.quizapp.repository.GridAttemptRepository;
import com.quizapp.repository.GridRepository;
import com.quizapp.repository.LineupAttemptRepository;
import com.quizapp.repository.LineupRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

// The standings arithmetic is unit-tested in WeeklyBoardStandingsTest; this runs the real queries
// (the attempts-with-user-and-solved-entries fetch) against the database end to end.
@SpringBootTest
class WeeklyBoardStandingsIntegrationTest {

    @Autowired private WeeklyBoardStandingsService service;
    @Autowired private GridRepository gridRepository;
    @Autowired private GridAttemptRepository gridAttemptRepository;
    @Autowired private LineupRepository lineupRepository;
    @Autowired private LineupAttemptRepository lineupAttemptRepository;
    @Autowired private AppUserRepository appUserRepository;
    @Autowired private AthleteRepository athleteRepository;

    // The suite shares one database and the battle-mode tests draw from every grid / lineup in it with
    // assumptions about their size, so leave nothing behind.
    private final java.util.List<Runnable> cleanup = new java.util.ArrayList<>();

    @org.junit.jupiter.api.AfterEach
    void removeWhatThisTestCreated() {
        cleanup.forEach(Runnable::run);
        cleanup.clear();
    }

    private AppUser newUser(String prefix) {
        String suffix = String.valueOf(System.nanoTime());
        AppUser u = new AppUser();
        u.setEmail(prefix + "-" + suffix + "@example.com");
        u.setName(prefix + " " + suffix);
        return appUserRepository.save(u);
    }

    private Athlete newAthlete(String sport) {
        Athlete a = new Athlete();
        a.setName("Athlete " + System.nanoTime());
        a.setSport(sport);
        return athleteRepository.save(a);
    }

    @Test
    void weeklyGridStandingsCountCompletedAttemptsMinusOvertime() {
        Grid g = new Grid();
        g.setTitle("Weekly " + System.nanoTime());
        g.setSport("Football");
        g.setWeekStartDate(LocalDate.now());
        g.setMaxStrikes(3);
        g.setExcludedFromGridBattle(true);
        Set<GridEntry> entries = new HashSet<>();
        for (int i = 0; i < 4; i++) {
            GridEntry e = new GridEntry();
            e.setAthlete(newAthlete("Football"));
            e.setOrderIndex(i);
            // Grid Battle draws from every grid in the shared test database and sorts by these.
            e.setHintLabel("Goals");
            e.setHintValue(10 + i);
            entries.add(e);
        }
        g.setEntries(entries);
        g = gridRepository.save(g);
        java.util.List<Long> entryIds = g.getEntries().stream().map(GridEntry::getId).toList();
        final Grid savedGrid = g;
        cleanup.add(() -> {
            gridAttemptRepository.findByGrid_Id(savedGrid.getId()).forEach(gridAttemptRepository::delete);
            gridRepository.deleteById(savedGrid.getId());
        });

        AppUser player = newUser("gridder");
        GridAttempt done = new GridAttempt();
        done.setGrid(g);
        done.setUser(player);
        done.setCompleted(true);
        done.setSolvedEntryIds(new HashSet<>(entryIds));                              // 4 found...
        done.setOvertimeSolvedEntryIds(new HashSet<>(entryIds.subList(0, 1)));       // ...1 of them in overtime
        gridAttemptRepository.save(done);

        AppUser unfinished = newUser("unfinished");
        GridAttempt open = new GridAttempt();
        open.setGrid(g);
        open.setUser(unfinished);
        open.setSolvedEntryIds(new HashSet<>(entryIds.subList(0, 2)));
        gridAttemptRepository.save(open);

        DailyQuizWeeklyDto dto = service.getGridWeekly(player.getEmail());

        var mine = dto.getCurrent().getStandings().stream().filter(s -> s.getPlayerName().equals(player.getName())).findFirst();
        assertThat(mine).isPresent();
        assertThat(mine.get().getTotal()).isEqualTo(3);
        assertThat(mine.get().isYou()).isTrue();
        assertThat(dto.getCurrent().getStandings()).noneMatch(s -> s.getPlayerName().equals(unfinished.getName()));
    }

    @Test
    void weeklyLineupStandingsCountCompletedAttempts() {
        Lineup l = new Lineup();
        l.setTitle("Lineup " + System.nanoTime());
        l.setTeamName("Arsenal");
        l.setOpponentName("Chelsea");
        l.setWeekStartDate(LocalDate.now());
        l.setFormation("4-3-3");
        l.setMaxStrikes(3);
        Set<LineupEntry> entries = new HashSet<>();
        for (int i = 0; i < 3; i++) {
            LineupEntry e = new LineupEntry();
            e.setAthlete(newAthlete(Lineup.CATEGORY));
            e.setSlotIndex(i);
            e.setShirtNumber(i + 1);
            entries.add(e);
        }
        l.setEntries(entries);
        l = lineupRepository.save(l);
        java.util.List<Long> entryIds = l.getEntries().stream().map(LineupEntry::getId).toList();
        final Lineup savedLineup = l;
        cleanup.add(() -> {
            lineupAttemptRepository.findByLineup_Id(savedLineup.getId()).forEach(lineupAttemptRepository::delete);
            lineupRepository.deleteById(savedLineup.getId());
        });

        AppUser player = newUser("xi");
        LineupAttempt done = new LineupAttempt();
        done.setLineup(l);
        done.setUser(player);
        done.setCompleted(true);
        done.setSolvedEntryIds(new HashSet<>(entryIds.subList(0, 2)));
        lineupAttemptRepository.save(done);

        DailyQuizWeeklyDto dto = service.getLineupWeekly(player.getEmail());

        var mine = dto.getCurrent().getStandings().stream().filter(s -> s.getPlayerName().equals(player.getName())).findFirst();
        assertThat(mine).isPresent();
        assertThat(mine.get().getTotal()).isEqualTo(2);
        assertThat(mine.get().getDaysPlayed()).isEqualTo(1);
    }
}
