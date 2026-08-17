package com.quizapp.dto;

import java.time.LocalDate;
import java.util.List;

public class LineupAdminDetailDto {

    private Long id;
    private String title;
    private String competition;
    private LocalDate matchDate;
    private LocalDate weekStartDate;
    private String formation;
    private String teamName;
    private String teamCrestUrl;
    private String opponentName;
    private String opponentCrestUrl;
    private Integer scoreFor;
    private Integer scoreAgainst;
    private int maxStrikes;
    private boolean excludedFromBattle;
    private String kitColor;
    private String goalkeeperKitColor;
    private List<AthleteDto> candidates;
    private List<EntryDetail> entries;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getCompetition() { return competition; }
    public void setCompetition(String competition) { this.competition = competition; }
    public LocalDate getMatchDate() { return matchDate; }
    public void setMatchDate(LocalDate matchDate) { this.matchDate = matchDate; }
    public LocalDate getWeekStartDate() { return weekStartDate; }
    public void setWeekStartDate(LocalDate weekStartDate) { this.weekStartDate = weekStartDate; }
    public String getFormation() { return formation; }
    public void setFormation(String formation) { this.formation = formation; }
    public String getTeamName() { return teamName; }
    public void setTeamName(String teamName) { this.teamName = teamName; }
    public String getTeamCrestUrl() { return teamCrestUrl; }
    public void setTeamCrestUrl(String teamCrestUrl) { this.teamCrestUrl = teamCrestUrl; }
    public String getOpponentName() { return opponentName; }
    public void setOpponentName(String opponentName) { this.opponentName = opponentName; }
    public String getOpponentCrestUrl() { return opponentCrestUrl; }
    public void setOpponentCrestUrl(String opponentCrestUrl) { this.opponentCrestUrl = opponentCrestUrl; }
    public Integer getScoreFor() { return scoreFor; }
    public void setScoreFor(Integer scoreFor) { this.scoreFor = scoreFor; }
    public Integer getScoreAgainst() { return scoreAgainst; }
    public void setScoreAgainst(Integer scoreAgainst) { this.scoreAgainst = scoreAgainst; }
    public int getMaxStrikes() { return maxStrikes; }
    public void setMaxStrikes(int maxStrikes) { this.maxStrikes = maxStrikes; }
    public boolean isExcludedFromBattle() { return excludedFromBattle; }
    public void setExcludedFromBattle(boolean excludedFromBattle) { this.excludedFromBattle = excludedFromBattle; }
    public String getKitColor() { return kitColor; }
    public void setKitColor(String kitColor) { this.kitColor = kitColor; }
    public String getGoalkeeperKitColor() { return goalkeeperKitColor; }
    public void setGoalkeeperKitColor(String goalkeeperKitColor) { this.goalkeeperKitColor = goalkeeperKitColor; }
    public List<AthleteDto> getCandidates() { return candidates; }
    public void setCandidates(List<AthleteDto> candidates) { this.candidates = candidates; }
    public List<EntryDetail> getEntries() { return entries; }
    public void setEntries(List<EntryDetail> entries) { this.entries = entries; }

    public static class EntryDetail {
        private Long id;
        private AthleteDto athlete;
        private int shirtNumber;
        private int slotIndex;
        private boolean captain;

        public EntryDetail(Long id, AthleteDto athlete, int shirtNumber, int slotIndex, boolean captain) {
            this.id = id;
            this.athlete = athlete;
            this.shirtNumber = shirtNumber;
            this.slotIndex = slotIndex;
            this.captain = captain;
        }

        public Long getId() { return id; }
        public AthleteDto getAthlete() { return athlete; }
        public int getShirtNumber() { return shirtNumber; }
        public int getSlotIndex() { return slotIndex; }
        public boolean isCaptain() { return captain; }
    }
}
