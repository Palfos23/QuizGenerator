package com.quizapp.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.util.List;

// The daily quiz's weekly competition: this week's running standings (Monday-Sunday, every graded
// day's score added up) and who won each of the last few finished weeks.
public class DailyQuizWeeklyDto {

    private WeekDto current;
    private List<PastWeekDto> pastWeeks;

    // The expert's total for the current week (shown apart from the ranking), or null when there's no
    // expert or they haven't played this week. Always null for the weekly games' standings.
    private ExpertWeek expert;

    public DailyQuizWeeklyDto(WeekDto current, List<PastWeekDto> pastWeeks) {
        this.current = current;
        this.pastWeeks = pastWeeks;
    }

    public ExpertWeek getExpert() { return expert; }
    public void setExpert(ExpertWeek expert) { this.expert = expert; }

    public static class ExpertWeek {
        private final String name;
        private final int total;
        private final int daysPlayed;

        public ExpertWeek(String name, int total, int daysPlayed) {
            this.name = name;
            this.total = total;
            this.daysPlayed = daysPlayed;
        }

        public String getName() { return name; }
        public int getTotal() { return total; }
        public int getDaysPlayed() { return daysPlayed; }
    }

    public WeekDto getCurrent() { return current; }
    public List<PastWeekDto> getPastWeeks() { return pastWeeks; }

    public static class WeekDto {
        private LocalDate weekStart;
        private LocalDate weekEnd;
        private List<StandingDto> standings;

        public WeekDto(LocalDate weekStart, LocalDate weekEnd, List<StandingDto> standings) {
            this.weekStart = weekStart;
            this.weekEnd = weekEnd;
            this.standings = standings;
        }

        public LocalDate getWeekStart() { return weekStart; }
        public LocalDate getWeekEnd() { return weekEnd; }
        public List<StandingDto> getStandings() { return standings; }
    }

    public static class StandingDto {
        private String playerName;
        private int total;
        private int daysPlayed;
        private boolean isYou;

        public StandingDto(String playerName, int total, int daysPlayed, boolean isYou) {
            this.playerName = playerName;
            this.total = total;
            this.daysPlayed = daysPlayed;
            this.isYou = isYou;
        }

        public String getPlayerName() { return playerName; }
        public int getTotal() { return total; }
        public int getDaysPlayed() { return daysPlayed; }

        // Explicit name - Jackson would otherwise send "you" (see ScoreboardJsonTest).
        @JsonProperty("isYou")
        public boolean isYou() { return isYou; }
    }

    public static class PastWeekDto {
        private LocalDate weekStart;
        private LocalDate weekEnd;
        private List<String> winners; // more than one when players tie on the top total
        private int winningScore;
        private boolean provisional; // some of that week's answers were still waiting on an admin - the result could still change

        public PastWeekDto(LocalDate weekStart, LocalDate weekEnd, List<String> winners, int winningScore, boolean provisional) {
            this.weekStart = weekStart;
            this.weekEnd = weekEnd;
            this.winners = winners;
            this.winningScore = winningScore;
            this.provisional = provisional;
        }

        public LocalDate getWeekStart() { return weekStart; }
        public LocalDate getWeekEnd() { return weekEnd; }
        public List<String> getWinners() { return winners; }
        public int getWinningScore() { return winningScore; }
        public boolean isProvisional() { return provisional; }
    }
}
