package com.quizapp.dto;

import com.quizapp.model.RoomStatus;

import java.util.List;

public class LineupBattleStateDto {
    private String roomCode;
    private RoomStatus status;
    private Long yourParticipantId;
    private List<LineupBattlePlayerStateDto> players;
    private int currentLineupIndex;
    private int totalLineups;
    private Long currentLineupId;
    private String lineupTitle;
    private String lineupCompetition;
    private String teamName;
    private String teamCrestUrl;
    private String opponentName;
    private String opponentCrestUrl;
    private Integer scoreFor;
    private Integer scoreAgainst;
    private String formation;
    private int maxStrikes;
    private String kitColor;
    private String goalkeeperKitColor;
    private List<LineupBattleSlotDto> slots;
    private Long currentTurnParticipantId;
    private boolean lineupComplete;
    private boolean finished;

    // Only meaningful for "Random" games - true while this round's starting
    // player still needs to pick one of lineupChoices before anything else
    // about the round exists yet.
    private boolean awaitingLineupChoice;
    private List<LineupSummaryDto> lineupChoices;
    private Long pickerParticipantId;

    public String getRoomCode() { return roomCode; }
    public void setRoomCode(String roomCode) { this.roomCode = roomCode; }
    public RoomStatus getStatus() { return status; }
    public void setStatus(RoomStatus status) { this.status = status; }
    public Long getYourParticipantId() { return yourParticipantId; }
    public void setYourParticipantId(Long yourParticipantId) { this.yourParticipantId = yourParticipantId; }
    public List<LineupBattlePlayerStateDto> getPlayers() { return players; }
    public void setPlayers(List<LineupBattlePlayerStateDto> players) { this.players = players; }
    public int getCurrentLineupIndex() { return currentLineupIndex; }
    public void setCurrentLineupIndex(int currentLineupIndex) { this.currentLineupIndex = currentLineupIndex; }
    public int getTotalLineups() { return totalLineups; }
    public void setTotalLineups(int totalLineups) { this.totalLineups = totalLineups; }
    public Long getCurrentLineupId() { return currentLineupId; }
    public void setCurrentLineupId(Long currentLineupId) { this.currentLineupId = currentLineupId; }
    public String getLineupTitle() { return lineupTitle; }
    public void setLineupTitle(String lineupTitle) { this.lineupTitle = lineupTitle; }
    public String getLineupCompetition() { return lineupCompetition; }
    public void setLineupCompetition(String lineupCompetition) { this.lineupCompetition = lineupCompetition; }
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
    public String getFormation() { return formation; }
    public void setFormation(String formation) { this.formation = formation; }
    public int getMaxStrikes() { return maxStrikes; }
    public void setMaxStrikes(int maxStrikes) { this.maxStrikes = maxStrikes; }
    public String getKitColor() { return kitColor; }
    public void setKitColor(String kitColor) { this.kitColor = kitColor; }
    public String getGoalkeeperKitColor() { return goalkeeperKitColor; }
    public void setGoalkeeperKitColor(String goalkeeperKitColor) { this.goalkeeperKitColor = goalkeeperKitColor; }
    public List<LineupBattleSlotDto> getSlots() { return slots; }
    public void setSlots(List<LineupBattleSlotDto> slots) { this.slots = slots; }
    public Long getCurrentTurnParticipantId() { return currentTurnParticipantId; }
    public void setCurrentTurnParticipantId(Long currentTurnParticipantId) { this.currentTurnParticipantId = currentTurnParticipantId; }
    public boolean isLineupComplete() { return lineupComplete; }
    public void setLineupComplete(boolean lineupComplete) { this.lineupComplete = lineupComplete; }
    public boolean isFinished() { return finished; }
    public void setFinished(boolean finished) { this.finished = finished; }
    public boolean isAwaitingLineupChoice() { return awaitingLineupChoice; }
    public void setAwaitingLineupChoice(boolean awaitingLineupChoice) { this.awaitingLineupChoice = awaitingLineupChoice; }
    public List<LineupSummaryDto> getLineupChoices() { return lineupChoices; }
    public void setLineupChoices(List<LineupSummaryDto> lineupChoices) { this.lineupChoices = lineupChoices; }
    public Long getPickerParticipantId() { return pickerParticipantId; }
    public void setPickerParticipantId(Long pickerParticipantId) { this.pickerParticipantId = pickerParticipantId; }
}
