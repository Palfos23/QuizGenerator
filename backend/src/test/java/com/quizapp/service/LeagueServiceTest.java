package com.quizapp.service;

import com.quizapp.dto.LeagueDtos.*;
import com.quizapp.exception.ResourceNotFoundException;
import com.quizapp.model.*;
import com.quizapp.repository.AppUserRepository;
import com.quizapp.repository.LeagueDayScoreRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.*;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// A league from creation to a drawn head-to-head season: joining by code, the draw on the first day, scores
// coming in, the deadline, who may do what, and the owner leaving. Uses its own users on a far-future clock.
@SpringBootTest
class LeagueServiceTest {

    @Autowired private LeagueService leagueService;
    @Autowired private LeagueScoreService leagueScoreService;
    @Autowired private LeagueDayScoreRepository dayScoreRepository;
    @Autowired private AppUserRepository appUserRepository;

    private static final LocalDate T = LocalDate.of(2032, 1, 12);
    private final List<AppUser> users = new ArrayList<>();

    private void at(LocalDate day) {
        leagueService.setClock(Clock.fixed(day.atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault()));
    }

    private AppUser user(String name) {
        AppUser u = new AppUser();
        u.setEmail("league-" + name + "-" + System.nanoTime() + "@example.com");
        u.setName(name);
        AppUser saved = appUserRepository.save(u);
        users.add(saved);
        return saved;
    }

    private void dayScore(AppUser u, LocalDate day, int score) {
        LeagueDayScore row = new LeagueDayScore();
        row.setUserId(u.getId());
        row.setQuizDate(day);
        row.setScore(score);
        row.setMaxScore(16);
        dayScoreRepository.save(row);
    }

    @BeforeEach
    void start() {
        at(T);
    }

    @AfterEach
    void cleanUp() {
        users.forEach(u -> {
            leagueService.removeUser(u.getId());
            appUserRepository.delete(u);
        });
        leagueService.setClock(Clock.systemDefaultZone());
    }

    @Test
    void aHeadToHeadLeagueFromCreationToTheFirstResults() {
        AppUser ada = user("Ada"), bo = user("Bo"), cy = user("Cy"), di = user("Di");
        LeagueDetail created = leagueService.create(ada.getEmail(),
                new CreateLeagueRequest("  Kontoret  ", LeagueFormat.HEAD_TO_HEAD, "TOMORROW", 2, true));
        assertThat(created.name()).isEqualTo("Kontoret");
        assertThat(created.owner()).isTrue();
        assertThat(created.season().state()).isEqualTo("UPCOMING");

        JoinPreview preview = leagueService.preview(bo.getEmail(), created.code().toLowerCase());
        assertThat(preview.joinsCurrentSeason()).isTrue();
        assertThat(preview.alreadyMember()).isFalse();
        leagueService.join(bo.getEmail(), created.code());
        leagueService.join(cy.getEmail(), created.code());
        leagueService.join(di.getEmail(), created.code());

        // The first day: the matchups are drawn - four players, two weeks, two matchups a day, then playoffs
        at(T.plusDays(1));
        LeagueDetail day1 = leagueService.detail(ada.getEmail(), created.id(), null);
        SeasonView season = day1.season();
        assertThat(season.scheduled()).isTrue();
        assertThat(season.participants()).hasSize(4);
        assertThat(season.days().stream().filter(d -> d.stage().equals("REGULAR"))).hasSize(14);
        assertThat(season.days().stream().filter(d -> d.stage().equals("REGULAR")).mapToInt(d -> d.matches().size()).sum()).isEqualTo(28);
        assertThat(season.endDate()).isEqualTo(T.plusDays(1).plusDays(14 + 2 - 1));
        MatchView adaToday = season.today().myMatch();
        assertThat(adaToday).isNotNull();
        assertThat(adaToday.status()).isEqualTo("LIVE");

        // Everyone plays day one; the next day the results are in the table
        dayScore(ada, T.plusDays(1), 12);
        dayScore(bo, T.plusDays(1), 9);
        dayScore(cy, T.plusDays(1), 9);
        dayScore(di, T.plusDays(1), 9);
        at(T.plusDays(2));
        SeasonView next = leagueService.detail(bo.getEmail(), created.id(), null).season();
        assertThat(next.table().get(0).player().name()).isEqualTo("Ada");
        assertThat(next.table().get(0).points()).isEqualTo(3);
        assertThat(next.table().stream().mapToInt(TableRow::points).sum()).isEqualTo(3 + 0 + 1 + 1);

        // The dashboard card names today's opponent
        LeagueCard card = leagueService.myLeagues(bo.getEmail()).get(0);
        assertThat(card.todayOpponent()).isNotNull();
        assertThat(card.inSeason()).isTrue();
        assertThat(card.myRank()).isNotNull();
    }

    @Test
    void lateJoinersWaitForTheNextSeasonAndOutsidersSeeNothing() {
        AppUser ada = user("Ada"), bo = user("Bo"), late = user("Late"), outsider = user("Out");
        LeagueDetail created = leagueService.create(ada.getEmail(),
                new CreateLeagueRequest("Liga", LeagueFormat.HEAD_TO_HEAD, "TODAY", 1, false));
        leagueService.join(bo.getEmail(), created.code());
        leagueService.detail(ada.getEmail(), created.id(), null); // drawn now: two players, starting today

        assertThat(leagueService.preview(late.getEmail(), created.code()).joinsCurrentSeason()).isFalse();
        LeagueDetail lateView = leagueService.join(late.getEmail(), created.code());
        assertThat(lateView.season().notice()).isEqualTo("NOT_IN_SEASON");
        assertThat(lateView.season().participants()).hasSize(2);

        assertThatThrownBy(() -> leagueService.detail(outsider.getEmail(), created.id(), null)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> leagueService.newSeason(bo.getEmail(), created.id(),
                new SeasonSettings(LeagueFormat.POINTS, "TOMORROW", null, false))).isInstanceOf(IllegalStateException.class);

        // The owner leaving hands the league to the next member
        leagueService.leave(ada.getEmail(), created.id());
        assertThat(leagueService.detail(bo.getEmail(), created.id(), null).owner()).isTrue();
    }

    @Test
    void aHeadToHeadSeasonWithOnePlayerWaitsForASecond() {
        AppUser ada = user("Ada"), bo = user("Bo");
        LeagueDetail created = leagueService.create(ada.getEmail(),
                new CreateLeagueRequest("Alene", LeagueFormat.HEAD_TO_HEAD, "TODAY", 1, true));
        assertThat(created.season().notice()).isEqualTo("NEEDS_PLAYERS");
        at(T.plusDays(3));
        leagueService.join(bo.getEmail(), created.code());
        SeasonView v = leagueService.detail(ada.getEmail(), created.id(), null).season();
        assertThat(v.scheduled()).isTrue();
        assertThat(v.startDate()).isEqualTo(T.plusDays(3)); // the start moved on until there were two
        assertThat(v.playoffs()).isFalse();                  // fewer than four players: no playoffs
    }

    @Test
    void onlyAQuizHandedInOnItsOwnDayCounts() {
        AppUser ada = user("Ada");
        leagueService.create(ada.getEmail(), new CreateLeagueRequest("Frist", LeagueFormat.POINTS, "TODAY", null, false));
        LocalDate day = T.minusDays(1);
        DailyQuizSet set = new DailyQuizSet();
        set.setQuizDate(day);
        DailyQuizAttempt attempt = new DailyQuizAttempt();
        attempt.setSet(set);
        attempt.setUser(ada);
        attempt.setStatus(DailyQuizAttemptStatus.GRADED);
        attempt.setScore(11);

        attempt.setSubmittedAt(day.plusDays(1).atTime(9, 0).atZone(ZoneId.systemDefault()).toInstant());
        leagueScoreService.record(attempt);
        assertThat(dayScoreRepository.findByUserIdAndQuizDate(ada.getId(), day)).isEmpty();

        attempt.setSubmittedAt(day.atTime(21, 30).atZone(ZoneId.systemDefault()).toInstant());
        leagueScoreService.record(attempt);
        assertThat(dayScoreRepository.findByUserIdAndQuizDate(ada.getId(), day)).get().extracting(LeagueDayScore::getScore).isEqualTo(11);
    }

    @Test
    void theOwnerCanReplaceASeasonBeforeItStartsButNotWhileItRuns() {
        AppUser ada = user("Ada");
        LeagueDetail created = leagueService.create(ada.getEmail(),
                new CreateLeagueRequest("Bytt", LeagueFormat.HEAD_TO_HEAD, "TOMORROW", 4, false));
        assertThat(created.canStartNewSeason()).isTrue();
        LeagueDetail replaced = leagueService.newSeason(ada.getEmail(), created.id(), new SeasonSettings(LeagueFormat.POINTS, "TODAY", 2, false));
        assertThat(replaced.seasons()).hasSize(1);
        assertThat(replaced.season().format()).isEqualTo(LeagueFormat.POINTS);
        assertThat(replaced.canStartNewSeason()).isFalse();
        assertThatThrownBy(() -> leagueService.newSeason(ada.getEmail(), created.id(), new SeasonSettings(LeagueFormat.POINTS, "TODAY", 2, false)))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> leagueService.create(ada.getEmail(), new CreateLeagueRequest(" ", LeagueFormat.POINTS, "TODAY", null, false)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
