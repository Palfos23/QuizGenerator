package com.quizapp.service;

import com.quizapp.dto.BdayNightDto.*;
import com.quizapp.dto.BdayGuestDto;
import com.quizapp.repository.BdayMarioKartTimeRepository;
import com.quizapp.repository.BdayWheelSpinRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// The birthday night beyond the quiz: Mario Kart best laps, music quiz teams, the wheel - and the combined totals.
@SpringBootTest
class BdayNightServiceTest {

    @Autowired private BdayNightService night;
    @Autowired private BdayStandingsService standings;
    @Autowired private BdayQuizService quiz;
    @Autowired private BdayMarioKartTimeRepository timeRepository;
    @Autowired private BdayWheelSpinRepository spinRepository;

    private final List<Long> teams = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        // laps, spins, teams and the settings are shared by the whole night - leave them as found
        timeRepository.deleteAll();
        spinRepository.deleteAll();
        standings.updateReveal(new RevealRequest(0, 0));
        teams.forEach(night::deleteTeam);
        teams.clear();
        standings.updateSettings(new Settings(false, BdayStandingsService.DEFAULT_MK_POINTS, BdayStandingsService.DEFAULT_MK_OTHER));
    }

    private BdayGuestDto guest(String prefix) {
        return quiz.addGuest(prefix + " " + System.nanoTime());
    }

    private void openMarioKart() {
        standings.updateSettings(new Settings(true, "10, 8, 6", 1));
    }

    private PlayerStanding row(Long guestId) {
        return standings.standings().players().stream().filter(p -> p.guestId().equals(guestId)).findFirst().orElseThrow();
    }

    // ------------------------------------------------------------------ the final reveal

    private List<BdayGuestDto> threeLeaders() {
        List<BdayGuestDto> leaders = new ArrayList<>();
        int bonus = 100; // nobody else in the shared test database gets near these totals
        for (String name : new String[] {"Gull", "Solv", "Bronse"}) {
            BdayGuestDto g = guest(name);
            night.recordSpin(new SpinRequest(g.getId(), bonus--, ""));
            leaders.add(g);
        }
        return leaders;
    }

    @Test
    void hiddenTopPlacesLeaveTheServerAsAnonymousTilesWithNoPoints() {
        List<BdayGuestDto> leaders = threeLeaders();
        standings.updateReveal(new RevealRequest(3, null));

        Standings pub = standings.publicStandings();

        assertThat(pub.reveal().masked()).isTrue();
        assertThat(pub.players().subList(0, 3)).allMatch(PlayerStanding::hidden)
                .allMatch(p -> p.guestId() == null && p.name() == null && p.teamName() == null && p.total() == 0 && p.bonusPoints() == 0);
        assertThat(pub.players().subList(0, 3)).extracting(PlayerStanding::rank).containsExactly(1, 2, 3);
        // no competition's points for anyone, and no per-competition lists
        assertThat(pub.players()).allMatch(p -> p.quizScore() == 0 && p.bonusPoints() == 0 && p.marioKartPoints() == 0 && p.musicPoints() == 0);
        assertThat(pub.teams()).isEmpty();
        assertThat(pub.marioKart()).isEmpty();
        // the host still sees everything
        assertThat(standings.standings().players().get(0).name()).isEqualTo(leaders.get(0).getName());
        assertThat(standings.standings().players().get(0).bonusPoints()).isEqualTo(100);
    }

    @Test
    void tilesTurnFromTheLowestHiddenPlaceUpToFirst() {
        List<BdayGuestDto> leaders = threeLeaders();
        standings.updateReveal(new RevealRequest(3, null));

        standings.revealNext();
        Standings one = standings.publicStandings();
        assertThat(one.players().get(2).hidden()).isFalse();
        assertThat(one.players().get(2).name()).isEqualTo(leaders.get(2).getName());
        assertThat(one.players().get(2).total()).isEqualTo(98);
        assertThat(one.players().get(2).bonusPoints()).isZero(); // the total, not where it came from
        assertThat(one.players().get(1).hidden()).isTrue();
        assertThat(one.players().get(0).hidden()).isTrue();

        standings.revealNext();
        standings.revealNext();
        Standings done = standings.publicStandings();
        assertThat(done.reveal().masked()).isFalse();
        assertThat(done.players().get(0).name()).isEqualTo(leaders.get(0).getName());
        assertThat(done.players().get(0).bonusPoints()).isEqualTo(100); // everything open once the last tile is turned
        assertThat(standings.revealNext().revealed()).isEqualTo(3); // nothing left to turn
    }

    @Test
    void changingHowManyPlacesAreHiddenTurnsEveryTileBack() {
        threeLeaders();
        standings.updateReveal(new RevealRequest(3, null));
        standings.revealNext();

        RevealState state = standings.updateReveal(new RevealRequest(2, null));

        assertThat(state.revealed()).isZero();
        assertThat(state.tiles()).isEqualTo(2);
        assertThat(standings.updateReveal(new RevealRequest(0, null)).masked()).isFalse();
        assertThatThrownBy(() -> standings.updateReveal(new RevealRequest(-1, null))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void whileTheTopIsHiddenMarioKartLapsAreShownWithoutWhatTheyAreWorth() {
        BdayGuestDto g = guest("Kart");
        openMarioKart();
        night.enterOwnTime(g.getId(), "1:30.000");

        assertThat(night.marioKartPublic().ranking()).extracting(LapEntry::points).doesNotContainNull();
        standings.updateReveal(new RevealRequest(1, null));
        assertThat(night.marioKartPublic().ranking()).extracting(LapEntry::points).containsOnlyNulls();
        assertThat(night.marioKartPublic().ranking()).extracting(LapEntry::timeMs).contains(90_000);
    }

    // ------------------------------------------------------------------ lap times

    @Test
    void lapTimesAcceptTheUsualWaysOfWritingThem() {
        assertThat(BdayNightService.parseLapTime("1:23.456")).isEqualTo(83_456);
        assertThat(BdayNightService.parseLapTime("1:23,45")).isEqualTo(83_450);
        assertThat(BdayNightService.parseLapTime("1:23.4")).isEqualTo(83_400);
        assertThat(BdayNightService.parseLapTime("83.456")).isEqualTo(83_456);
        assertThat(BdayNightService.parseLapTime("  0:59.999 ")).isEqualTo(59_999);
        assertThat(BdayNightService.parseLapTime("1:23")).isEqualTo(83_000);
    }

    @Test
    void nonsenseLapTimesAreRejected() {
        for (String bad : new String[] {"", "abc", "1:2:3", "1:75.000", "-1:00.000", "0:01.000", "99:00.000", "1:23.4567", "1;23.456"}) {
            assertThatThrownBy(() -> BdayNightService.parseLapTime(bad)).as(bad).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    void thePointsTableIsParsedAndValidated() {
        assertThat(BdayStandingsService.parsePointsTable("10, 8;6  5")).containsExactly(10, 8, 6, 5);
        assertThatThrownBy(() -> BdayStandingsService.parsePointsTable("")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> BdayStandingsService.parsePointsTable("10, ti")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> BdayStandingsService.parsePointsTable("10, -2")).isInstanceOf(IllegalArgumentException.class);
        assertThat(BdayStandingsService.pointsForRank(1, List.of(10, 8), 1)).isEqualTo(10);
        assertThat(BdayStandingsService.pointsForRank(2, List.of(10, 8), 1)).isEqualTo(8);
        assertThat(BdayStandingsService.pointsForRank(3, List.of(10, 8), 1)).isEqualTo(1);
    }

    // ------------------------------------------------------------------ Mario Kart entry and ranking

    @Test
    void guestsCanOnlyEnterTimesWhileRegistrationIsOpenAndOnlyOnce() {
        BdayGuestDto g = guest("Kart");

        assertThatThrownBy(() -> night.enterOwnTime(g.getId(), "1:30.000")).isInstanceOf(IllegalStateException.class); // closed
        openMarioKart();
        assertThat(night.enterOwnTime(g.getId(), "1:30.000").timeMs()).isEqualTo(90_000);
        assertThatThrownBy(() -> night.enterOwnTime(g.getId(), "1:20.000")).isInstanceOf(IllegalStateException.class); // already has one
        assertThat(timeRepository.findByGuest_Id(g.getId()).orElseThrow().getTimeMs()).isEqualTo(90_000);
        assertThatThrownBy(() -> night.enterOwnTime(guest("Dårlig").getId(), "tjue sekunder")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void theHostCanFixOrRemoveATimeAndTheGuestThenHasNone() {
        openMarioKart();
        BdayGuestDto g = guest("Fix");
        night.enterOwnTime(g.getId(), "1:30.000");

        assertThat(night.setTime(g.getId(), "1:10.500").timeMs()).isEqualTo(70_500);
        assertThat(night.marioKartPublic().guests()).filteredOn(s -> s.id().equals(g.getId())).allMatch(GuestSlot::hasTime);

        night.clearTime(g.getId());
        assertThat(night.marioKartPublic().guests()).filteredOn(s -> s.id().equals(g.getId())).noneMatch(GuestSlot::hasTime);
        assertThat(row(g.getId()).marioKartPoints()).isZero();
        // ...and registration works again for them
        assertThat(night.enterOwnTime(g.getId(), "1:25.000").rank()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void fasterLapsRankHigherAndTiesShareARankAndItsPoints() {
        openMarioKart(); // 10, 8, 6 - and 1 for everyone else
        BdayGuestDto a = guest("A"), b = guest("B"), c = guest("C"), d = guest("D"), e = guest("E");
        night.enterOwnTime(a.getId(), "1:20.000"); // 1st
        night.enterOwnTime(b.getId(), "1:25.000"); // tied 2nd
        night.enterOwnTime(c.getId(), "1:25.000"); // tied 2nd
        night.enterOwnTime(d.getId(), "1:40.000"); // 4th - past the 3-place table, so 1
        // e never enters a time

        assertThat(row(a.getId()).marioKartRank()).isEqualTo(1);
        assertThat(row(a.getId()).marioKartPoints()).isEqualTo(10);
        assertThat(row(b.getId()).marioKartRank()).isEqualTo(2);
        assertThat(row(b.getId()).marioKartPoints()).isEqualTo(8);
        assertThat(row(c.getId()).marioKartPoints()).isEqualTo(8);
        assertThat(row(d.getId()).marioKartRank()).isEqualTo(4);
        assertThat(row(d.getId()).marioKartPoints()).isEqualTo(1);
        assertThat(row(e.getId()).marioKartPoints()).isZero();
        assertThat(row(e.getId()).marioKartTimeMs()).isNull();
        assertThat(standings.lapRanking()).extracting(LapEntry::guestId).containsExactly(a.getId(), b.getId(), c.getId(), d.getId());
    }

    @Test
    void changingThePointsTableRescoresEveryone() {
        openMarioKart();
        BdayGuestDto a = guest("A"), b = guest("B");
        night.enterOwnTime(a.getId(), "1:20.000");
        night.enterOwnTime(b.getId(), "1:30.000");
        assertThat(row(b.getId()).marioKartPoints()).isEqualTo(8);

        standings.updateSettings(new Settings(true, "20, 15", 2));

        assertThat(row(a.getId()).marioKartPoints()).isEqualTo(20);
        assertThat(row(b.getId()).marioKartPoints()).isEqualTo(15);
        assertThatThrownBy(() -> standings.updateSettings(new Settings(true, "ti, åtte", 1))).isInstanceOf(IllegalArgumentException.class);
        assertThat(standings.settings().marioKartPoints()).isEqualTo("20, 15"); // a bad update changes nothing
    }

    // ------------------------------------------------------------------ teams

    @Test
    void aTeamsMusicScoreCountsForEveryMemberAndOnlyThem() {
        BdayGuestDto a = guest("Lag1"), b = guest("Lag2"), outsider = guest("Utenfor");
        Team team = createTeam("Rockerne " + System.nanoTime());
        night.assignTeam(a.getId(), team.id());
        night.assignTeam(b.getId(), team.id());

        night.updateTeam(team.id(), new TeamRequest(team.name(), 12));

        assertThat(row(a.getId()).musicPoints()).isEqualTo(12);
        assertThat(row(b.getId()).musicPoints()).isEqualTo(12);
        assertThat(row(a.getId()).teamName()).isEqualTo(team.name());
        assertThat(row(outsider.getId()).musicPoints()).isZero();
        assertThat(standings.standings().teams()).filteredOn(t -> t.id().equals(team.id())).allMatch(t -> t.members() == 2 && t.musicScore() == 12);
    }

    @Test
    void teamNamesAreUniqueAndADeletedTeamReleasesItsMembers() {
        BdayGuestDto a = guest("Lag");
        Team team = createTeam("Duplikat " + System.nanoTime());
        assertThatThrownBy(() -> night.createTeam(new TeamRequest(team.name().toUpperCase(), 0))).isInstanceOf(IllegalArgumentException.class);
        night.assignTeam(a.getId(), team.id());
        night.updateTeam(team.id(), new TeamRequest(team.name(), 7));

        night.deleteTeam(team.id());
        teams.remove(team.id());

        assertThat(row(a.getId()).teamName()).isNull();
        assertThat(row(a.getId()).musicPoints()).isZero();
        assertThat(night.teamsAdmin().unassigned()).extracting(Person::id).contains(a.getId());
    }

    @Test
    void aGuestCanBeMovedBetweenTeamsAndOffThem() {
        BdayGuestDto a = guest("Flytt");
        Team one = createTeam("Ett " + System.nanoTime()), two = createTeam("To " + System.nanoTime());
        night.updateTeam(one.id(), new TeamRequest(one.name(), 3));
        night.updateTeam(two.id(), new TeamRequest(two.name(), 9));

        night.assignTeam(a.getId(), one.id());
        assertThat(row(a.getId()).musicPoints()).isEqualTo(3);
        night.assignTeam(a.getId(), two.id());
        assertThat(row(a.getId()).musicPoints()).isEqualTo(9);
        night.assignTeam(a.getId(), null);
        assertThat(row(a.getId()).musicPoints()).isZero();
    }

    private Team createTeam(String name) {
        TeamsAdmin admin = night.createTeam(new TeamRequest(name, 0));
        Team t = admin.teams().stream().filter(x -> x.name().equals(name)).findFirst().orElseThrow();
        teams.add(t.id());
        return t;
    }

    // ------------------------------------------------------------------ the wheel

    @Test
    void everyoneSpinsOnceAndOnlyThoseWhoHaventAreOfferedNext() {
        BdayGuestDto a = guest("Hjul");
        BdayGuestDto stillWaiting = guest("Venter"); // somebody left to pick

        assertThat(night.wheelAdmin().candidates()).extracting(Person::id).contains(a.getId(), stillWaiting.getId());
        night.recordSpin(new SpinRequest(a.getId(), 5, "Bonus"));

        assertThat(night.wheelAdmin().candidates()).extracting(Person::id).doesNotContain(a.getId());
        assertThat(night.wheelAdmin().spins()).anyMatch(s -> s.guestId().equals(a.getId()) && s.bonusPoints() == 5 && "Bonus".equals(s.note()));
        assertThatThrownBy(() -> night.recordSpin(new SpinRequest(a.getId(), 3, null))).isInstanceOf(IllegalStateException.class);
        for (int i = 0; i < 25; i++) { // the pick is random - never someone who has already spun
            assertThat(night.pickNextSpinner().id()).isNotEqualTo(a.getId());
        }
        assertThat(row(a.getId()).bonusPoints()).isEqualTo(5);
    }

    @Test
    void theWheelCanGiveNegativePointsButNotAbsurdOnes() {
        BdayGuestDto a = guest("Minus");
        night.recordSpin(new SpinRequest(a.getId(), -4, "Bomben"));
        assertThat(row(a.getId()).bonusPoints()).isEqualTo(-4);

        assertThatThrownBy(() -> night.updateSpin(a.getId(), new SpinRequest(a.getId(), 500, null))).isInstanceOf(IllegalArgumentException.class);
        night.updateSpin(a.getId(), new SpinRequest(a.getId(), 10, "Jackpot"));
        assertThat(row(a.getId()).bonusPoints()).isEqualTo(10);
    }

    @Test
    void removingASpinGivesTheTurnBack() {
        BdayGuestDto a = guest("Tilbake");
        night.recordSpin(new SpinRequest(a.getId(), 6, null));
        night.deleteSpin(a.getId());

        assertThat(night.wheelAdmin().candidates()).extracting(Person::id).contains(a.getId());
        assertThat(row(a.getId()).bonusPoints()).isZero();
        night.recordSpin(new SpinRequest(a.getId(), 2, null)); // can spin again
    }

    @Test
    void thePickerSaysSoWhenEveryoneHasSpun() {
        timeRepository.deleteAll();
        spinRepository.deleteAll();
        for (Person p : night.wheelAdmin().candidates()) night.recordSpin(new SpinRequest(p.id(), 1, null));

        assertThatThrownBy(() -> night.pickNextSpinner()).isInstanceOf(IllegalStateException.class);
    }

    // ------------------------------------------------------------------ the combined board

    @Test
    void theTotalIsQuizPlusMarioKartPlusMusicPlusBonus() {
        openMarioKart();
        BdayGuestDto g = guest("Sum");
        Team team = createTeam("Sumlag " + System.nanoTime());
        night.assignTeam(g.getId(), team.id());
        night.updateTeam(team.id(), new TeamRequest(team.name(), 5));
        night.enterOwnTime(g.getId(), "1:15.000"); // fastest of the night in this test: 10 points
        night.recordSpin(new SpinRequest(g.getId(), 3, null));

        // before the quiz: no quiz points
        PlayerStanding before = row(g.getId());
        assertThat(before.quizDone()).isFalse();
        assertThat(before.total()).isEqualTo(10 + 5 + 3);

        quiz.submitAnswers(g.getId(), new com.quizapp.dto.BdaySubmitRequest());
        quiz.setScore(g.getId(), 7);

        PlayerStanding after = row(g.getId());
        assertThat(after.quizDone()).isTrue();
        assertThat(after.quizScore()).isEqualTo(7);
        assertThat(after.total()).isEqualTo(7 + 10 + 5 + 3);
    }

    @Test
    void theBoardIsRankedByTotalWithEqualTotalsSharingARank() {
        BdayGuestDto a = guest("Rang1"), b = guest("Rang2"), c = guest("Rang3");
        night.recordSpin(new SpinRequest(a.getId(), 90, null));
        night.recordSpin(new SpinRequest(b.getId(), 80, null));
        night.recordSpin(new SpinRequest(c.getId(), 80, null));

        assertThat(row(a.getId()).rank()).isLessThan(row(b.getId()).rank());
        assertThat(row(b.getId()).rank()).isEqualTo(row(c.getId()).rank());
        List<Integer> totals = standings.standings().players().stream().filter(p -> p.rank() > 0).map(PlayerStanding::total).toList();
        assertThat(totals).isSortedAccordingTo(java.util.Comparator.reverseOrder());
    }

    @Test
    void guestsWithNothingRegisteredHaveNoPlaceAndSitBelowEveryoneElse() {
        BdayGuestDto idle = guest("Ingenting");
        BdayGuestDto negative = guest("Minus");
        night.recordSpin(new SpinRequest(negative.getId(), -5, null));

        night.assignTeam(idle.getId(), createTeam("Lag uten poeng " + System.nanoTime()).id()); // being on a team with no score yet is not taking part
        PlayerStanding none = row(idle.getId());
        assertThat(none.rank()).isZero();
        assertThat(none.total()).isZero();
        assertThat(row(negative.getId()).rank()).isGreaterThan(0); // took part, even if below zero

        List<PlayerStanding> all = standings.standings().players();
        assertThat(all.indexOf(none)).isGreaterThan(all.indexOf(row(negative.getId())));
        assertThat(all.subList(all.indexOf(none), all.size())).allMatch(p -> p.rank() == 0);
    }

    @Test
    void removingAWaitingGuestAlsoRemovesTheirLapAndSpin() {
        openMarioKart();
        BdayGuestDto g = guest("Borte");
        night.enterOwnTime(g.getId(), "1:30.000");
        night.recordSpin(new SpinRequest(g.getId(), 4, null));

        quiz.deleteGuest(g.getId());

        assertThat(timeRepository.findByGuest_Id(g.getId())).isEmpty();
        assertThat(spinRepository.findByGuest_Id(g.getId())).isEmpty();
        assertThat(standings.standings().players()).noneMatch(p -> p.guestId().equals(g.getId()));
    }
}
