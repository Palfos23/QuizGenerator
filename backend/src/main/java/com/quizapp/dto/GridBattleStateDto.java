package com.quizapp.dto;

import com.quizapp.model.RoomStatus;

import java.util.List;

public class GridBattleStateDto {
    private String roomCode;
    private RoomStatus status;
    private Long yourParticipantId;
    private List<GridBattlePlayerStateDto> players;
    private int currentGridIndex;
    private int totalGrids;
    private Long currentGridId;
    private String gridTitle;
    private String gridTheme;
    private int maxStrikes;
    private boolean fitImages; // true = fit whole tile image (flags); false = cover/crop
    private List<GridBattleEntryDto> entries;
    private Long currentTurnParticipantId;
    private boolean gridComplete;
    private boolean finished;

    // Only meaningful for "Random" games - true while this round's starting
    // player still needs to pick one of gridChoices before anything else about
    // the round (entries, strikes, etc.) exists yet.
    private boolean awaitingGridChoice;
    private List<GridSummaryDto> gridChoices;
    private Long pickerParticipantId;

    public String getRoomCode() { return roomCode; }
    public void setRoomCode(String roomCode) { this.roomCode = roomCode; }
    public RoomStatus getStatus() { return status; }
    public void setStatus(RoomStatus status) { this.status = status; }
    public Long getYourParticipantId() { return yourParticipantId; }
    public void setYourParticipantId(Long yourParticipantId) { this.yourParticipantId = yourParticipantId; }
    public List<GridBattlePlayerStateDto> getPlayers() { return players; }
    public void setPlayers(List<GridBattlePlayerStateDto> players) { this.players = players; }
    public int getCurrentGridIndex() { return currentGridIndex; }
    public void setCurrentGridIndex(int currentGridIndex) { this.currentGridIndex = currentGridIndex; }
    public int getTotalGrids() { return totalGrids; }
    public void setTotalGrids(int totalGrids) { this.totalGrids = totalGrids; }
    public Long getCurrentGridId() { return currentGridId; }
    public void setCurrentGridId(Long currentGridId) { this.currentGridId = currentGridId; }
    public String getGridTitle() { return gridTitle; }
    public void setGridTitle(String gridTitle) { this.gridTitle = gridTitle; }
    public String getGridTheme() { return gridTheme; }
    public void setGridTheme(String gridTheme) { this.gridTheme = gridTheme; }
    public int getMaxStrikes() { return maxStrikes; }
    public void setMaxStrikes(int maxStrikes) { this.maxStrikes = maxStrikes; }
    public boolean isFitImages() { return fitImages; }
    public void setFitImages(boolean fitImages) { this.fitImages = fitImages; }
    public List<GridBattleEntryDto> getEntries() { return entries; }
    public void setEntries(List<GridBattleEntryDto> entries) { this.entries = entries; }
    public Long getCurrentTurnParticipantId() { return currentTurnParticipantId; }
    public void setCurrentTurnParticipantId(Long currentTurnParticipantId) { this.currentTurnParticipantId = currentTurnParticipantId; }
    public boolean isGridComplete() { return gridComplete; }
    public void setGridComplete(boolean gridComplete) { this.gridComplete = gridComplete; }
    public boolean isFinished() { return finished; }
    public void setFinished(boolean finished) { this.finished = finished; }
    public boolean isAwaitingGridChoice() { return awaitingGridChoice; }
    public void setAwaitingGridChoice(boolean awaitingGridChoice) { this.awaitingGridChoice = awaitingGridChoice; }
    public List<GridSummaryDto> getGridChoices() { return gridChoices; }
    public void setGridChoices(List<GridSummaryDto> gridChoices) { this.gridChoices = gridChoices; }
    public Long getPickerParticipantId() { return pickerParticipantId; }
    public void setPickerParticipantId(Long pickerParticipantId) { this.pickerParticipantId = pickerParticipantId; }
}
