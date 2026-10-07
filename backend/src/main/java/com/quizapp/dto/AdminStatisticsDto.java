package com.quizapp.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Everything the admin Statistics page renders, assembled in one round trip by
 * {@link com.quizapp.service.StatisticsService}. Deliberately a plain read-only
 * snapshot - no pagination or filtering, since every list here is small (game
 * modes, categories, this week's grids).
 */
public class AdminStatisticsDto {

    private long totalUsers;
    private long totalSubjects;
    private long totalCategories;

    // New sign-ups per calendar month, oldest first, covering the last 12 months
    // that had at least one registration (months with none are still included so
    // a chart doesn't misrepresent the gaps).
    private List<CountEntry> usersByMonth;

    // One row per game mode - "how many boards exist for each".
    private List<CountEntry> boardsByGameMode;

    // One row per battle game mode - "how many games have ever been played",
    // online and pass-and-play combined. See GamePlayEvent.
    private List<CountEntry> battleGamesPlayed;

    // Subjects (athletes) grouped by their category, biggest first.
    private List<CountEntry> subjectsByCategory;

    // Weekly grids grouped by category, biggest first.
    private List<CountEntry> gridsByCategory;

    // Tension questions grouped by their main category.
    private List<CountEntry> tensionQuestionsByCategory;

    // Stats for every grid whose live week covers today - usually one per
    // category. Empty when nothing is running this week.
    private List<WeeklyGridStat> weeklyGrids;

    // --- Added with the Statistics redesign ---

    private long totalQuestions;
    // Questions in the shared bank per language, biggest first.
    private List<CountEntry> questionsByLanguage;

    // "Needs attention": things waiting on an admin.
    private long pendingSubmissions;
    private long openReports;
    private long dailyQuizPendingReviews;

    // Distinct players who did anything this week (Monday to now): a daily quiz result, or started a
    // weekly grid / Starting XI board. And sign-ups in the last 7 days.
    private long activePlayersThisWeek;
    private long newUsersLast7Days;

    // The last 14 days of the daily quiz, oldest first: how many players finished and their average.
    private List<DailyQuizDayStat> dailyQuizActivity;

    // Same shape as weeklyGrids, for the Starting XI boards live this week (category = "Team vs Opponent").
    private List<WeeklyGridStat> weeklyLineups;

    public long getTotalQuestions() { return totalQuestions; }
    public void setTotalQuestions(long totalQuestions) { this.totalQuestions = totalQuestions; }
    public List<CountEntry> getQuestionsByLanguage() { return questionsByLanguage; }
    public void setQuestionsByLanguage(List<CountEntry> questionsByLanguage) { this.questionsByLanguage = questionsByLanguage; }
    public long getPendingSubmissions() { return pendingSubmissions; }
    public void setPendingSubmissions(long pendingSubmissions) { this.pendingSubmissions = pendingSubmissions; }
    public long getOpenReports() { return openReports; }
    public void setOpenReports(long openReports) { this.openReports = openReports; }
    public long getDailyQuizPendingReviews() { return dailyQuizPendingReviews; }
    public void setDailyQuizPendingReviews(long dailyQuizPendingReviews) { this.dailyQuizPendingReviews = dailyQuizPendingReviews; }
    public long getActivePlayersThisWeek() { return activePlayersThisWeek; }
    public void setActivePlayersThisWeek(long activePlayersThisWeek) { this.activePlayersThisWeek = activePlayersThisWeek; }
    public long getNewUsersLast7Days() { return newUsersLast7Days; }
    public void setNewUsersLast7Days(long newUsersLast7Days) { this.newUsersLast7Days = newUsersLast7Days; }
    public List<DailyQuizDayStat> getDailyQuizActivity() { return dailyQuizActivity; }
    public void setDailyQuizActivity(List<DailyQuizDayStat> dailyQuizActivity) { this.dailyQuizActivity = dailyQuizActivity; }
    public List<WeeklyGridStat> getWeeklyLineups() { return weeklyLineups; }
    public void setWeeklyLineups(List<WeeklyGridStat> weeklyLineups) { this.weeklyLineups = weeklyLineups; }

    public static class DailyQuizDayStat {
        private final LocalDate date;
        private final int players;
        private final double averagePercent; // average score as a percentage of the maximum, 0 when nobody played

        public DailyQuizDayStat(LocalDate date, int players, double averagePercent) {
            this.date = date;
            this.players = players;
            this.averagePercent = averagePercent;
        }

        public LocalDate getDate() { return date; }
        public int getPlayers() { return players; }
        public double getAveragePercent() { return averagePercent; }
    }

    public long getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(long totalUsers) {
        this.totalUsers = totalUsers;
    }

    public long getTotalSubjects() {
        return totalSubjects;
    }

    public void setTotalSubjects(long totalSubjects) {
        this.totalSubjects = totalSubjects;
    }

    public long getTotalCategories() {
        return totalCategories;
    }

    public void setTotalCategories(long totalCategories) {
        this.totalCategories = totalCategories;
    }

    public List<CountEntry> getUsersByMonth() {
        return usersByMonth;
    }

    public void setUsersByMonth(List<CountEntry> usersByMonth) {
        this.usersByMonth = usersByMonth;
    }

    public List<CountEntry> getBoardsByGameMode() {
        return boardsByGameMode;
    }

    public void setBoardsByGameMode(List<CountEntry> boardsByGameMode) {
        this.boardsByGameMode = boardsByGameMode;
    }

    public List<CountEntry> getBattleGamesPlayed() {
        return battleGamesPlayed;
    }

    public void setBattleGamesPlayed(List<CountEntry> battleGamesPlayed) {
        this.battleGamesPlayed = battleGamesPlayed;
    }

    public List<CountEntry> getSubjectsByCategory() {
        return subjectsByCategory;
    }

    public void setSubjectsByCategory(List<CountEntry> subjectsByCategory) {
        this.subjectsByCategory = subjectsByCategory;
    }

    public List<CountEntry> getGridsByCategory() {
        return gridsByCategory;
    }

    public void setGridsByCategory(List<CountEntry> gridsByCategory) {
        this.gridsByCategory = gridsByCategory;
    }

    public List<CountEntry> getTensionQuestionsByCategory() {
        return tensionQuestionsByCategory;
    }

    public void setTensionQuestionsByCategory(List<CountEntry> tensionQuestionsByCategory) {
        this.tensionQuestionsByCategory = tensionQuestionsByCategory;
    }

    public List<WeeklyGridStat> getWeeklyGrids() {
        return weeklyGrids;
    }

    public void setWeeklyGrids(List<WeeklyGridStat> weeklyGrids) {
        this.weeklyGrids = weeklyGrids;
    }

    /** A single labelled tally - reused by every breakdown list on the page. */
    public static class CountEntry {
        private String label;
        private long count;

        public CountEntry(String label, long count) {
            this.label = label;
            this.count = count;
        }

        public String getLabel() {
            return label;
        }

        public long getCount() {
            return count;
        }
    }

    /** This week's results for one grid. Score = correct answers found within the player's lives (overtime solves excluded), matching the weekly scoreboard. */
    public static class WeeklyGridStat {
        private Long gridId;
        private String title;
        private String category;
        private LocalDate weekStartDate;
        private int entryCount;
        private int players;
        private double averageScore;
        private int lowestScore;
        private int highestScore;

        public WeeklyGridStat(Long gridId, String title, String category, LocalDate weekStartDate, int entryCount,
                              int players, double averageScore, int lowestScore, int highestScore) {
            this.gridId = gridId;
            this.title = title;
            this.category = category;
            this.weekStartDate = weekStartDate;
            this.entryCount = entryCount;
            this.players = players;
            this.averageScore = averageScore;
            this.lowestScore = lowestScore;
            this.highestScore = highestScore;
        }

        public Long getGridId() {
            return gridId;
        }

        public String getTitle() {
            return title;
        }

        public String getCategory() {
            return category;
        }

        public LocalDate getWeekStartDate() {
            return weekStartDate;
        }

        public int getEntryCount() {
            return entryCount;
        }

        public int getPlayers() {
            return players;
        }

        public double getAverageScore() {
            return averageScore;
        }

        public int getLowestScore() {
            return lowestScore;
        }

        public int getHighestScore() {
            return highestScore;
        }
    }
}
