package com.quizapp.dto;

import com.quizapp.model.LeagueFormat;

import java.time.LocalDate;
import java.util.List;

/**
 * Everything the league pages send and receive. One file of small records rather than a dozen classes.
 * Player names can be null: a deleted account (the page shows "Slettet spiller"), or the Expert (expert = true;
 * the page shows its own label).
 */
public final class LeagueDtos {

    private LeagueDtos() {
    }

    // ---------- requests ----------

    /** start: "TODAY", "TOMORROW" or a date (yyyy-mm-dd, up to 90 days ahead). weeks: season length; null for a points season with no end. */
    public record SeasonSettings(LeagueFormat format, String start, Integer weeks, boolean playoffs) {
    }

    public record CreateLeagueRequest(String name, LeagueFormat format, String start, Integer weeks, boolean playoffs) {
        public SeasonSettings settings() {
            return new SeasonSettings(format, start, weeks, playoffs);
        }
    }

    public record JoinLeagueRequest(String code) {
    }

    // ---------- shared pieces ----------

    public record PlayerRef(Long id, String name, boolean me, boolean expert, boolean left) {
    }

    /** One row of a season table. form: head-to-head "W"/"D"/"L"; points the last days' scores ("-" missed, "…" in review). */
    public record TableRow(int rank, PlayerRef player, int played, int wins, int draws, int losses, int points,
                           int totalScore, Double average, Integer best, List<String> form) {
    }

    /**
     * One matchup. state*: SCORED, PENDING (answers in review), MISSED (did not hand in that day), NOT_YET (today,
     * still to play), UPCOMING. status: UPCOMING, LIVE (today, someone still to play), PENDING (waiting for review),
     * FINAL. outcome (FINAL only): A, B, DRAW, BOTH_LOST. Playoff matchups also carry seeds, the side that goes
     * through (advancing: A or B - a draw goes to the better seed), and a placeholder for a side not known yet
     * (SEED_1..SEED_4, WINNER_SEMI_1, WINNER_SEMI_2, LOSER_SEMI_1, LOSER_SEMI_2).
     */
    public record MatchView(LocalDate date, String stage, PlayerRef a, PlayerRef b, Integer scoreA, Integer scoreB,
                            String stateA, String stateB, String status, String outcome, Integer seedA, Integer seedB,
                            String placeholderA, String placeholderB, String advancing) {
    }

    /** A points-league day: one player's result. */
    public record DayScore(PlayerRef player, String state, Integer score) {
    }

    /** stage: REGULAR, SEMI, FINAL (the final and the bronze match) or DAY (points). */
    public record DayView(LocalDate date, String stage, List<MatchView> matches, List<PlayerRef> byes, List<DayScore> scores) {
    }

    public record StatRow(PlayerRef player, int played, Double average, Integer best, int wins, int draws, int losses,
                          int beatExpert, int currentStreak, int longestStreak) {
    }

    /**
     * kind: BEST_DAY (value = score, date), BEST_AVERAGE (average, value = days played), MOST_PLAYED (value = days,
     * otherValue = days in the season), MOST_WINS (value), LONGEST_WIN_STREAK (value), BIGGEST_WIN (value - otherValue,
     * other = the loser, date), BEAT_EXPERT (value = days).
     */
    public record Highlight(String kind, PlayerRef player, PlayerRef other, Integer value, Integer otherValue,
                            Double average, LocalDate date) {
    }

    public record RecapView(boolean provisional, PlayerRef champion, PlayerRef runnerUp, PlayerRef third,
                            List<Highlight> highlights, TableRow myRow, StatRow myStats, int participants) {
    }

    /** Today in this season: my matchup (head-to-head) and how many have played. */
    public record TodayView(LocalDate date, MatchView myMatch, boolean iPlayed, int playedCount, int participantCount) {
    }

    /**
     * state: UPCOMING, ACTIVE, FINISHED. notice: NEEDS_PLAYERS (head-to-head without two players yet - the start
     * moves on day by day until someone joins), NOT_IN_SEASON (I joined after the matchups were drawn).
     */
    public record SeasonView(int number, LeagueFormat format, LocalDate startDate, LocalDate endDate,
                             LocalDate regularEndDate, Integer regularWeeks, boolean playoffs, String state,
                             boolean settled, boolean scheduled, String notice, List<PlayerRef> participants,
                             List<TableRow> table, List<DayView> days, List<StatRow> stats, RecapView recap,
                             TodayView today) {
    }

    public record SeasonSummary(int number, LeagueFormat format, LocalDate startDate, LocalDate endDate, String state) {
    }

    public record MemberView(PlayerRef player, boolean owner, boolean inCurrentSeason) {
    }

    /** My all-time head-to-head record against one opponent, over every season of this league (playoffs included). */
    public record RivalryRow(PlayerRef opponent, int wins, int draws, int losses) {
    }

    public record LeagueDetail(Long id, String name, String code, boolean owner, boolean canStartNewSeason,
                               List<MemberView> members, List<SeasonSummary> seasons, SeasonView season,
                               List<RivalryRow> rivalries) {
    }

    /**
     * A league as the list and the dashboard show it. todayOpponent and the played flags are the nudge:
     * "your opponent has played today, you haven't". recapReady: the season ended in the last two weeks.
     */
    public record LeagueCard(Long id, String name, LeagueFormat format, int seasonNumber, String state,
                             LocalDate startDate, LocalDate endDate, int memberCount, int participantCount,
                             boolean inSeason, Integer myRank, PlayerRef todayOpponent, boolean opponentPlayed,
                             boolean iPlayed, int playedToday, boolean recapReady, PlayerRef champion) {
    }

    /** What an invite link shows before joining. joinsCurrentSeason: false once a head-to-head season has drawn its matchups. */
    public record JoinPreview(Long id, String name, String code, LeagueFormat format, String state, LocalDate startDate,
                              LocalDate endDate, int memberCount, boolean alreadyMember, boolean joinsCurrentSeason) {
    }

    // ---------- the admin's overview ----------

    /** One league as the admin's list shows it. played/possible: day scores in the last 7 finished days of its current season. */
    public record AdminLeagueRow(Long id, String name, String code, String ownerName, int memberCount, LeagueFormat format,
                                 int seasonNumber, String state, LocalDate startDate, LocalDate endDate, boolean playoffs,
                                 java.time.Instant createdAt, int participants, int playedLast7, int possibleLast7) {
    }

    /**
     * Totals across every league. Format and state counts go by each league's current season. playedPercentLast7: of all
     * the days the current seasons' players could have played in the last week, how many they did (null: none to count).
     */
    public record AdminLeagueOverview(int leagues, int activeLeagues, int upcomingLeagues, int finishedLeagues,
                                      int memberships, int players, double averageMembers, int headToHead, int points,
                                      int withPlayoffs, int createdLast30, int joinedLast30, Integer playedPercentLast7,
                                      List<AdminLeagueRow> rows) {
    }
}
