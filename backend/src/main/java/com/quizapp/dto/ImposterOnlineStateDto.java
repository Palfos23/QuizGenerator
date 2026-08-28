package com.quizapp.dto;

import com.quizapp.model.RoomStatus;

import java.util.List;

public class ImposterOnlineStateDto {
    private String roomCode;
    private RoomStatus status;
    private Long yourParticipantId;
    private List<ImposterOnlinePlayerStateDto> players;
    private int currentGridIndex;
    private int totalGrids;
    private Long currentGridId;
    private String gridTitle;
    private String gridDescription;
    private String displayMode;
    private boolean fitImages;
    private int imposterCount;
    private List<ImposterOnlineTileDto> tiles;
    private Long currentTurnParticipantId;
    private boolean boardComplete;
    private boolean finished;

    // Only meaningful for "Random" games - true while this round's starting
    // player still needs to pick one of gridChoices before anything else
    // about the round exists yet.
    private boolean awaitingGridChoice;
    private List<ImposterGridSummaryDto> gridChoices;
    private Long pickerParticipantId;

    public String getRoomCode() { return roomCode; }
    public void setRoomCode(String roomCode) { this.roomCode = roomCode; }
    public RoomStatus getStatus() { return status; }
    public void setStatus(RoomStatus status) { this.status = status; }
    public Long getYourParticipantId() { return yourParticipantId; }
    public void setYourParticipantId(Long yourParticipantId) { this.yourParticipantId = yourParticipantId; }
    public List<ImposterOnlinePlayerStateDto> getPlayers() { return players; }
    public void setPlayers(List<ImposterOnlinePlayerStateDto> players) { this.players = players; }
    public int getCurrentGridIndex() { return currentGridIndex; }
    public void setCurrentGridIndex(int currentGridIndex) { this.currentGridIndex = currentGridIndex; }
    public int getTotalGrids() { return totalGrids; }
    public void setTotalGrids(int totalGrids) { this.totalGrids = totalGrids; }
    public Long getCurrentGridId() { return currentGridId; }
    public void setCurrentGridId(Long currentGridId) { this.currentGridId = currentGridId; }
    public String getGridTitle() { return gridTitle; }
    public void setGridTitle(String gridTitle) { this.gridTitle = gridTitle; }
    public String getGridDescription() { return gridDescription; }
    public void setGridDescription(String gridDescription) { this.gridDescription = gridDescription; }
    public String getDisplayMode() { return displayMode; }
    public void setDisplayMode(String displayMode) { this.displayMode = displayMode; }
    public boolean isFitImages() { return fitImages; }
    public void setFitImages(boolean fitImages) { this.fitImages = fitImages; }
    public int getImposterCount() { return imposterCount; }
    public void setImposterCount(int imposterCount) { this.imposterCount = imposterCount; }
    public List<ImposterOnlineTileDto> getTiles() { return tiles; }
    public void setTiles(List<ImposterOnlineTileDto> tiles) { this.tiles = tiles; }
    public Long getCurrentTurnParticipantId() { return currentTurnParticipantId; }
    public void setCurrentTurnParticipantId(Long currentTurnParticipantId) { this.currentTurnParticipantId = currentTurnParticipantId; }
    public boolean isBoardComplete() { return boardComplete; }
    public void setBoardComplete(boolean boardComplete) { this.boardComplete = boardComplete; }
    public boolean isFinished() { return finished; }
    public void setFinished(boolean finished) { this.finished = finished; }
    public boolean isAwaitingGridChoice() { return awaitingGridChoice; }
    public void setAwaitingGridChoice(boolean awaitingGridChoice) { this.awaitingGridChoice = awaitingGridChoice; }
    public List<ImposterGridSummaryDto> getGridChoices() { return gridChoices; }
    public void setGridChoices(List<ImposterGridSummaryDto> gridChoices) { this.gridChoices = gridChoices; }
    public Long getPickerParticipantId() { return pickerParticipantId; }
    public void setPickerParticipantId(Long pickerParticipantId) { this.pickerParticipantId = pickerParticipantId; }
}
