package com.quizapp.service;

import com.quizapp.dto.DailyQuizWeeklyDto;
import com.quizapp.service.WeeklyBoardStandingsService.Row;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WeeklyBoardStandingsTest {

    // Monday 5 Oct 2026
    private static final LocalDate THIS_MONDAY = LocalDate.of(2026, 10, 5);

    private static Row row(long userId, String name, LocalDate weekStart, int score, boolean include) {
        return new Row(userId, name, name.toLowerCase() + "@example.com", weekStart, score, include);
    }

    @Test
    void weekStartIsTheMondayOfTheWeek() {
        assertThat(WeeklyBoardStandingsService.weekStart(LocalDate.of(2026, 10, 11))).isEqualTo(THIS_MONDAY); // Sunday
        assertThat(WeeklyBoardStandingsService.weekStart(THIS_MONDAY)).isEqualTo(THIS_MONDAY);
    }

    @Test
    void sumsAPlayersBoardsWithinTheWeekAndRanksBestFirst() {
        List<Row> rows = List.of(
                row(1, "Anna", THIS_MONDAY, 8, true),
                row(1, "Anna", THIS_MONDAY, 7, true),   // second board the same week
                row(2, "Bo", THIS_MONDAY, 12, true),
                row(3, "Cy", THIS_MONDAY, 9, true));

        DailyQuizWeeklyDto dto = WeeklyBoardStandingsService.build(rows, "cy@example.com", THIS_MONDAY);

        List<DailyQuizWeeklyDto.StandingDto> s = dto.getCurrent().getStandings();
        assertThat(s).extracting(DailyQuizWeeklyDto.StandingDto::getPlayerName).containsExactly("Anna", "Bo", "Cy");
        assertThat(s.get(0).getTotal()).isEqualTo(15);
        assertThat(s.get(0).getDaysPlayed()).isEqualTo(2); // boards completed
        assertThat(s).extracting(DailyQuizWeeklyDto.StandingDto::isYou).containsExactly(false, false, true);
        assertThat(dto.getCurrent().getWeekStart()).isEqualTo(THIS_MONDAY);
        assertThat(dto.getCurrent().getWeekEnd()).isEqualTo(THIS_MONDAY.plusDays(6));
        assertThat(dto.getPastWeeks()).isEmpty();
    }

    @Test
    void optedOutPlayersAreHiddenExceptFromThemselves() {
        List<Row> rows = List.of(row(1, "Anna", THIS_MONDAY, 10, false), row(2, "Bo", THIS_MONDAY, 5, true));

        assertThat(WeeklyBoardStandingsService.build(rows, "bo@example.com", THIS_MONDAY).getCurrent().getStandings())
                .extracting(DailyQuizWeeklyDto.StandingDto::getPlayerName).containsExactly("Bo");
        assertThat(WeeklyBoardStandingsService.build(rows, "anna@example.com", THIS_MONDAY).getCurrent().getStandings())
                .extracting(DailyQuizWeeklyDto.StandingDto::getPlayerName).containsExactly("Anna", "Bo");
    }

    @Test
    void pastWeeksNameTheWinnersAndTiesShareTheWin() {
        LocalDate lastMonday = THIS_MONDAY.minusWeeks(1);
        LocalDate twoAgo = THIS_MONDAY.minusWeeks(2);
        List<Row> rows = List.of(
                row(1, "Anna", lastMonday, 10, true), row(2, "Bo", lastMonday, 10, true), row(3, "Cy", lastMonday, 4, true),
                row(3, "Cy", twoAgo, 6, true), row(1, "Anna", twoAgo, 5, false)); // Anna opted out: not named

        List<DailyQuizWeeklyDto.PastWeekDto> past = WeeklyBoardStandingsService.build(rows, "anna@example.com", THIS_MONDAY).getPastWeeks();

        assertThat(past).hasSize(2);
        assertThat(past.get(0).getWeekStart()).isEqualTo(lastMonday);
        assertThat(past.get(0).getWinners()).containsExactly("Anna", "Bo");
        assertThat(past.get(0).getWinningScore()).isEqualTo(10);
        assertThat(past.get(1).getWinners()).containsExactly("Cy");
        assertThat(past.get(1).isProvisional()).isFalse();
    }
}
