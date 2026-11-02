package com.quizapp.dto;

import java.time.LocalDate;
import java.util.List;

/** The shapes the advent calendar (player side and admin side) sends and receives. */
public final class AdventDto {

    private AdventDto() {}

    // ---------------------------------------------------------------- the calendar page

    /** One door in the grid. type/points/matches are only there once they're meant to be known. */
    public record DoorDto(int day, LocalDate date, String status, boolean entered, String type, Integer points, Integer matches) {}

    /**
     * phase: BEFORE (coming soon) | RUNNING | FINISHED. doors: empty before the start. Door status: LOCKED (opens
     * later) | OPEN (today's) | CLOSED (over, results out) | EMPTY (the game master never filled this one in).
     */
    public record StateDto(String phase, int year, LocalDate startDate, int daysUntilStart, Integer todayDay,
                           boolean player, List<DoorDto> doors, int jokersLeft, int jokersTotal, int totalPoints,
                           Integer rank, int streak, int bestStreak, boolean showName,
                           boolean wrappedAvailable, LocalDate simulatedDate) {}

    // ---------------------------------------------------------------- one door

    public record EntryDto(List<String> answers, boolean risky, boolean jokerUsed) {}

    /** What happened to one hidden answer: how many of the players matched it. */
    public record HiddenAnswerDto(String text, int matchedPlayers, int matchedPercent, boolean wildcard) {}

    /** One of the answers players wrote most often. matchedIndex = the hidden answer it counted as, or null. */
    public record CrowdAnswerDto(String text, int count, int percent, Integer matchedIndex) {}

    /** One player's result on a closed door. answerMatches: per answer they wrote, the hidden answer it matched (or -1). */
    public record ResultDto(List<Integer> answerMatches, int matches, boolean wildcardHit, int basePoints, int jokerBonus,
                            int points) {}

    public record DayDetailDto(int day, LocalDate date, String status, String category, String type, boolean finale,
                               EntryDto entry, int jokersLeft, boolean jokerAllowed,
                               ResultDto result, List<HiddenAnswerDto> hidden, int entrants, int gamemasterPoints,
                               boolean provisional, List<CrowdAnswerDto> crowd) {}

    public record EntryRequest(List<String> answers, boolean risky, boolean useJoker) {}

    public record ProfileRequest(boolean showName) {}

    // ---------------------------------------------------------------- leaderboard and wrapped

    public record StandingDto(int rank, String name, int points, boolean you) {}

    public record LeaderboardDto(List<StandingDto> players, int gamemasterPoints, int playedDays, Integer yourRank,
                                 int participants, boolean showName) {}

    public record BestDayDto(int day, String category, int points) {}

    public record WrappedDto(int year, int totalPoints, Integer rank, int participants, int daysPlayed, int daysTotal,
                             BestDayDto bestDay, int bestStreak, int syncPercent, int matchesTotal, int jokersUsed,
                             int jokerBonusPoints, int risksTaken, int risksWon, int wildcardHits, int perfectDays,
                             int gamemasterPoints, List<StandingDto> podium) {}

    // ---------------------------------------------------------------- admin

    public record AdminDayRowDto(int day, LocalDate date, boolean ready, String category, String type, long entries,
                                 boolean approved, boolean closed) {}

    public record AdminSeasonDto(int year, LocalDate today, List<AdminDayRowDto> days) {}

    public record AdminDayDto(int day, LocalDate date, String category, String type, List<String> answers,
                              Integer wildcardIndex, long entries, boolean approved, boolean closed, boolean locked) {}

    public record AdminDayRequest(String category, String type, List<String> answers, Integer wildcardIndex) {}

    /** One distinct thing players wrote on a door and how it is currently read. */
    public record ReviewAnswerDto(String text, String normalized, int count, int matchedIndex, boolean corrected) {}

    public record ReviewDto(int day, String category, String type, List<String> hidden, int entrants, boolean approved,
                            boolean closed, int gamemasterPoints, List<ReviewAnswerDto> answers) {}

    /** Read this text as hidden answer answerIndex (0-3), as no match (-1), or - null - as the automatic reading again. */
    public record DecisionRequest(String text, Integer answerIndex) {}

    public record ApproveRequest(boolean approved) {}

    /** The game master's test mode: a pretend date (for a few hours) and whether the test data may be wiped. */
    public record TestModeDto(boolean active, LocalDate date, java.time.Instant until, LocalDate realToday, boolean canReset) {}

    /** date null = back to the real date. */
    public record TestDateRequest(LocalDate date) {}
}
