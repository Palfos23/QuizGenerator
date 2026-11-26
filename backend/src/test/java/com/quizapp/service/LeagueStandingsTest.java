package com.quizapp.service;

import com.quizapp.dto.LeagueDtos.*;
import com.quizapp.model.LeagueFormat;
import com.quizapp.model.LeagueMatch;
import com.quizapp.model.LeagueSeason;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

// The league rules on their own, without a database: the draw, head-to-head results (the deadline, draws, both
// missing), the table, playoffs and the recap.
class LeagueStandingsTest {

    private static final LocalDate D = LocalDate.of(2031, 6, 2);

    private static LeagueSeason h2h(int weeks, boolean playoffs) {
        LeagueSeason s = new LeagueSeason();
        s.setNumber(1);
        s.setFormat(LeagueFormat.HEAD_TO_HEAD);
        s.setStartDate(D);
        s.setRegularWeeks(weeks);
        s.setPlayoffs(playoffs);
        s.recalculateEndDate();
        s.setScheduled(true);
        return s;
    }

    private static LeagueStandings.Input input(LeagueSeason s, LocalDate today, Long... players) {
        LeagueStandings.Input in = new LeagueStandings.Input();
        in.season = s;
        in.today = today;
        in.participants = List.of(players);
        Map<Long, String> names = new HashMap<>();
        for (Long p : players) names.put(p, "P" + p);
        in.names = names;
        in.scores = new HashMap<>();
        in.pending = new HashSet<>();
        return in;
    }

    private static void score(LeagueStandings.Input in, long user, LocalDate day, int value) {
        in.scores.computeIfAbsent(user, k -> new HashMap<>()).put(day, value);
    }

    @Test
    void theDrawPairsEveryoneWithEveryoneOncePerCycle() {
        List<List<long[]>> draw = LeagueStandings.roundRobin(List.of(1L, 2L, 3L, 4L), 3, false);
        Set<String> pairs = new HashSet<>();
        for (List<long[]> day : draw) {
            assertThat(day).hasSize(2);
            Set<Long> playing = new HashSet<>();
            for (long[] p : day) {
                playing.add(p[0]);
                playing.add(p[1]);
                pairs.add(Math.min(p[0], p[1]) + "-" + Math.max(p[0], p[1]));
            }
            assertThat(playing).containsExactlyInAnyOrder(1L, 2L, 3L, 4L);
        }
        assertThat(pairs).hasSize(6);
    }

    @Test
    void theOddOneOutMeetsTheExpertOrSitsOut() {
        List<List<long[]>> withExpert = LeagueStandings.roundRobin(List.of(1L, 2L, 3L), 3, true);
        Map<Long, Integer> vsExpert = new HashMap<>();
        for (List<long[]> day : withExpert) {
            assertThat(day).hasSize(2);
            for (long[] p : day) {
                assertThat(p[0]).isNotEqualTo(LeagueStandings.EXPERT); // the Expert is always side b
                if (p[1] == LeagueStandings.EXPERT) vsExpert.merge(p[0], 1, Integer::sum);
            }
        }
        assertThat(vsExpert).containsOnlyKeys(1L, 2L, 3L).allSatisfy((k, v) -> assertThat(v).isEqualTo(1));

        List<List<long[]>> noExpert = LeagueStandings.roundRobin(List.of(1L, 2L, 3L), 3, false);
        noExpert.forEach(day -> assertThat(day).hasSize(1));
    }

    @Test
    void headToHeadResultsFollowTheRules() {
        LeagueSeason s = h2h(1, false);
        LeagueStandings.Input in = input(s, D.plusDays(2), 1L, 2L, 3L, 4L);
        in.matches = List.of(
                new LeagueMatch(s, D, 1L, 2L), new LeagueMatch(s, D, 3L, 4L),
                new LeagueMatch(s, D.plusDays(1), 1L, 3L), new LeagueMatch(s, D.plusDays(1), 2L, 4L));
        score(in, 1, D, 10);
        score(in, 2, D, 8);              // 3 and 4 never handed in on day 1: both lose
        score(in, 1, D.plusDays(1), 5);
        score(in, 3, D.plusDays(1), 5);  // a draw
        score(in, 2, D.plusDays(1), 7);
        in.pending.add(LeagueStandings.pendingKey(4L, D.plusDays(1))); // 4's answers wait for an admin

        SeasonView v = LeagueStandings.compute(in);
        List<MatchView> day1 = v.days().get(0).matches();
        assertThat(day1.get(0).outcome()).isEqualTo("A");
        assertThat(day1.get(1).outcome()).isEqualTo("BOTH_LOST");
        List<MatchView> day2 = v.days().get(1).matches();
        assertThat(day2.get(0).outcome()).isEqualTo("DRAW");
        assertThat(day2.get(1).status()).isEqualTo("PENDING");
        assertThat(day2.get(1).outcome()).isNull();

        // 1: win + draw = 4. 3: loss + draw = 1. 2 and 4: one loss each (2-v-4 waits) - 2 ahead on quiz score.
        assertThat(v.table()).extracting(r -> r.player().id()).containsExactly(1L, 3L, 2L, 4L);
        assertThat(v.table()).extracting(TableRow::points).containsExactly(4, 1, 0, 0);
        assertThat(v.table().get(0).form()).containsExactly("W", "D");
        assertThat(v.table().get(3).played()).isZero();
    }

    @Test
    void todayIsLiveUntilBothHavePlayed() {
        LeagueSeason s = h2h(1, false);
        LeagueStandings.Input in = input(s, D, 1L, 2L);
        in.meId = 2L;
        in.matches = List.of(new LeagueMatch(s, D, 1L, 2L));
        score(in, 1, D, 9);

        SeasonView v = LeagueStandings.compute(in);
        assertThat(v.state()).isEqualTo("ACTIVE");
        assertThat(v.today().myMatch().status()).isEqualTo("LIVE");
        assertThat(v.today().myMatch().stateA()).isEqualTo("SCORED");
        assertThat(v.today().myMatch().stateB()).isEqualTo("NOT_YET");
        assertThat(v.today().iPlayed()).isFalse();
        assertThat(v.today().playedCount()).isEqualTo(1);
    }

    @Test
    void playoffsSendTheBetterSeedThroughOnADrawAndCrownAChampion() {
        LeagueSeason s = h2h(1, true);
        LocalDate semi = s.regularEndDate().plusDays(1);
        LocalDate fin = semi.plusDays(1);
        LeagueStandings.Input in = input(s, fin.plusDays(1), 1L, 2L, 3L, 4L);
        in.matches = List.of();
        // Regular-season totals decide the seeds: 1, 2, 3, 4
        score(in, 1, D, 14);
        score(in, 2, D, 12);
        score(in, 3, D, 10);
        score(in, 4, D, 8);
        score(in, 1, semi, 5);
        score(in, 4, semi, 9);   // 4 knocks out the top seed
        score(in, 2, semi, 6);
        score(in, 3, semi, 6);   // a draw: seed 2 goes through
        score(in, 2, fin, 3);
        score(in, 4, fin, 3);    // a draw in the final: seed 2 wins it
        score(in, 3, fin, 2);    // 1 doesn't play the bronze match: 3 takes third

        SeasonView v = LeagueStandings.compute(in);
        assertThat(v.state()).isEqualTo("FINISHED");
        assertThat(v.settled()).isTrue();
        List<MatchView> semis = v.days().stream().filter(d -> d.stage().equals("SEMI")).findFirst().orElseThrow().matches();
        assertThat(semis.get(0).advancing()).isEqualTo("B");
        assertThat(semis.get(1).advancing()).isEqualTo("A");
        RecapView recap = v.recap();
        assertThat(recap.champion().id()).isEqualTo(2L);
        assertThat(recap.runnerUp().id()).isEqualTo(4L);
        assertThat(recap.third().id()).isEqualTo(3L);
        // Playoff days don't change the regular-season table
        assertThat(v.table().get(0).totalScore()).isEqualTo(14);
    }

    @Test
    void aPointsSeasonAddsUpEveryDayAndMissedDaysScoreNothing() {
        LeagueSeason s = new LeagueSeason();
        s.setNumber(1);
        s.setFormat(LeagueFormat.POINTS);
        s.setStartDate(D);
        s.setEndDate(D.plusDays(2));
        LeagueStandings.Input in = input(s, D.plusDays(3), 1L, 2L);
        in.meId = 2L;
        score(in, 1, D, 10);
        score(in, 2, D, 12);
        score(in, 1, D.plusDays(1), 11); // 2 missed this day
        score(in, 1, D.plusDays(2), 3);
        score(in, 2, D.plusDays(2), 9);

        SeasonView v = LeagueStandings.compute(in);
        assertThat(v.table()).extracting(TableRow::totalScore).containsExactly(24, 21);
        assertThat(v.table().get(1).form()).containsExactly("12", "-", "9");
        assertThat(v.recap().champion().id()).isEqualTo(1L);
        assertThat(v.recap().myRow().rank()).isEqualTo(2);
        Highlight best = v.recap().highlights().stream().filter(h -> h.kind().equals("BEST_DAY")).findFirst().orElseThrow();
        assertThat(best.player().id()).isEqualTo(2L);
        assertThat(best.value()).isEqualTo(12);
    }

    @Test
    void aFinishedSeasonWithAnswersInReviewIsOnlyProvisional() {
        LeagueSeason s = h2h(1, false);
        LeagueStandings.Input in = input(s, s.getEndDate().plusDays(1), 1L, 2L);
        in.matches = List.of(new LeagueMatch(s, s.getEndDate(), 1L, 2L));
        score(in, 1, s.getEndDate(), 4);
        in.pending.add(LeagueStandings.pendingKey(2L, s.getEndDate()));

        SeasonView v = LeagueStandings.compute(in);
        assertThat(v.state()).isEqualTo("FINISHED");
        assertThat(v.settled()).isFalse();
        assertThat(v.recap().provisional()).isTrue();
        assertThat(v.days().stream().flatMap(d -> d.matches().stream()).map(MatchView::status).collect(Collectors.toList()))
                .containsExactly("PENDING");
    }
}
