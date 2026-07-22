package com.quizapp.dto;

import com.quizapp.model.Sport;

import java.time.LocalDate;
import java.util.List;

public class GridAdminDetailDto {

    private Long id;
    private String title;
    private String theme;
    private Sport sport;
    private LocalDate weekStartDate;
    private int maxStrikes;
    private List<AthleteDto> candidates;
    private List<EntryDetail> entries;

    public static class EntryDetail {
        private Long id;
        private AthleteDto athlete;
        private String hintLabel;
        private int hintValue;
        private ClubDto club;
        private boolean showLogo;

        public EntryDetail(Long id, AthleteDto athlete, String hintLabel, int hintValue, ClubDto club, boolean showLogo) {
            this.id = id;
            this.athlete = athlete;
            this.hintLabel = hintLabel;
            this.hintValue = hintValue;
            this.club = club;
            this.showLogo = showLogo;
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

        public int getHintValue() {
            return hintValue;
        }

        public ClubDto getClub() {
            return club;
        }

        public boolean isShowLogo() {
            return showLogo;
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

    public Sport getSport() {
        return sport;
    }

    public void setSport(Sport sport) {
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
