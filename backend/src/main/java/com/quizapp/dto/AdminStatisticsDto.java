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

    // Subjects (athletes) grouped by their category, biggest first.
    private List<CountEntry> subjectsByCategory;

    // Weekly grids grouped by category, biggest first.
    private List<CountEntry> gridsByCategory;

    // Tension questions grouped by their main category.
    private List<CountEntry> tensionQuestionsByCategory;

    // Stats for every grid whose live week covers today - usually one per
    // category. Empty when nothing is running this week.
    private List<WeeklyGridStat> weeklyGrids;

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
