package com.quizapp.dto;


import java.time.LocalDate;
import java.util.List;

public class GridAdminDetailDto {

    private Long id;
    private String title;
    private String theme;
    private String sport;
    private LocalDate weekStartDate;
    private int maxStrikes;
    private boolean sortAscending;
    private boolean ranked;
    private boolean excludedFromGridBattle;
    private String revealMode;
    private boolean entireCategoryPool;
    private List<AthleteDto> candidates;
    private List<EntryDetail> entries;

    public static class EntryDetail {
        private Long id;
        private AthleteDto athlete;
        private String hintLabel;
        private Integer hintValue;
        private ClubDto club;
        private boolean showLogo;
        private boolean useOwnPhotoAsLogo;
        private Long selectedPhotoId;
        private Long selectedDescriptionId;

        public EntryDetail(Long id, AthleteDto athlete, String hintLabel, Integer hintValue, ClubDto club,
                            boolean showLogo, boolean useOwnPhotoAsLogo, Long selectedPhotoId, Long selectedDescriptionId) {
            this.id = id;
            this.athlete = athlete;
            this.hintLabel = hintLabel;
            this.hintValue = hintValue;
            this.club = club;
            this.showLogo = showLogo;
            this.useOwnPhotoAsLogo = useOwnPhotoAsLogo;
            this.selectedPhotoId = selectedPhotoId;
            this.selectedDescriptionId = selectedDescriptionId;
        }

        public Long getSelectedPhotoId() {
            return selectedPhotoId;
        }

        public Long getSelectedDescriptionId() {
            return selectedDescriptionId;
        }

        public Long getId() {
            return id;
        }

        public AthleteDto getAthlete() {
            return athlete;
        }

        public String getHintLabel() {
            return hintLabel;
        }

        public Integer getHintValue() {
            return hintValue;
        }

        public ClubDto getClub() {
            return club;
        }

        public boolean isShowLogo() {
            return showLogo;
        }

        public boolean isUseOwnPhotoAsLogo() {
            return useOwnPhotoAsLogo;
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTheme() {
        return theme;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }

    public String getSport() {
        return sport;
    }

    public void setSport(String sport) {
        this.sport = sport;
    }

    public LocalDate getWeekStartDate() {
        return weekStartDate;
    }

    public void setWeekStartDate(LocalDate weekStartDate) {
        this.weekStartDate = weekStartDate;
    }

    public int getMaxStrikes() {
        return maxStrikes;
    }

    public void setMaxStrikes(int maxStrikes) {
        this.maxStrikes = maxStrikes;
    }

    public boolean isSortAscending() {
        return sortAscending;
    }

    public boolean isRanked() {
        return ranked;
    }

    public void setRanked(boolean ranked) {
        this.ranked = ranked;
    }

    public void setSortAscending(boolean sortAscending) {
        this.sortAscending = sortAscending;
    }

    public boolean isExcludedFromGridBattle() {
        return excludedFromGridBattle;
    }

    public void setExcludedFromGridBattle(boolean excludedFromGridBattle) {
        this.excludedFromGridBattle = excludedFromGridBattle;
    }

    public String getRevealMode() {
        return revealMode;
    }

    public void setRevealMode(String revealMode) {
        this.revealMode = revealMode;
    }

    public boolean isEntireCategoryPool() {
        return entireCategoryPool;
    }

    public void setEntireCategoryPool(boolean entireCategoryPool) {
        this.entireCategoryPool = entireCategoryPool;
    }

    public List<AthleteDto> getCandidates() {
        return candidates;
    }

    public void setCandidates(List<AthleteDto> candidates) {
        this.candidates = candidates;
    }

    public List<EntryDetail> getEntries() {
        return entries;
    }

    public void setEntries(List<EntryDetail> entries) {
        this.entries = entries;
    }
}
