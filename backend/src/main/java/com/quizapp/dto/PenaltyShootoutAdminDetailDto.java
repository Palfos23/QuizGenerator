package com.quizapp.dto;

import java.time.LocalDate;
import java.util.List;

public class PenaltyShootoutAdminDetailDto {

    private Long id;
    private String title;
    private String competition;
    private LocalDate matchDate;
    private String teamName;
    private String teamCrestUrl;
    private String opponentName;
    private String opponentCrestUrl;
    private Integer teamPensScored;
    private Integer opponentPensScored;
    private int maxStrikes;
    private List<AthleteDto> candidates;
    private List<KickDetail> kicks;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getCompetition() { return competition; }
    public void setCompetition(String competition) { this.competition = competition; }
    public LocalDate getMatchDate() { return matchDate; }
    public void setMatchDate(LocalDate matchDate) { this.matchDate = matchDate; }
    public String getTeamName() { return teamName; }
    public void setTeamName(String teamName) { this.teamName = teamName; }
    public String getTeamCrestUrl() { return teamCrestUrl; }
    public void setTeamCrestUrl(String teamCrestUrl) { this.teamCrestUrl = teamCrestUrl; }
    public String getOpponentName() { return opponentName; }
    public void setOpponentName(String opponentName) { this.opponentName = opponentName; }
    public String getOpponentCrestUrl() { return opponentCrestUrl; }
    public void setOpponentCrestUrl(String opponentCrestUrl) { this.opponentCrestUrl = opponentCrestUrl; }
    public Integer getTeamPensScored() { return teamPensScored; }
    public void setTeamPensScored(Integer teamPensScored) { this.teamPensScored = teamPensScored; }
    public Integer getOpponentPensScored() { return opponentPensScored; }
    public void setOpponentPensScored(Integer opponentPensScored) { this.opponentPensScored = opponentPensScored; }
    public int getMaxStrikes() { return maxStrikes; }
    public void setMaxStrikes(int maxStrikes) { this.maxStrikes = maxStrikes; }
    public List<AthleteDto> getCandidates() { return candidates; }
    public void setCandidates(List<AthleteDto> candidates) { this.candidates = candidates; }
    public List<KickDetail> getKicks() { return kicks; }
    public void setKicks(List<KickDetail> kicks) { this.kicks = kicks; }

    public static class KickDetail {
        private Long id;
        private AthleteDto athlete;
        private int kickOrder;
        private boolean forTeam;
        private boolean scored;

        public KickDetail(Long id, AthleteDto athlete, int kickOrder, boolean forTeam, boolean scored) {
            this.id = id;
            this.athlete = athlete;
            this.kickOrder = kickOrder;
            this.forTeam = forTeam;
            this.scored = scored;
        }

        public Long getId() { return id; }
        public AthleteDto getAthlete() { return athlete; }
        public int getKickOrder() { return kickOrder; }
        public boolean isForTeam() { return forTeam; }
        public boolean isScored() { return scored; }
    }
}
