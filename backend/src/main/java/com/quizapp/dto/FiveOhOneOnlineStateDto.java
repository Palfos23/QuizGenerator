package com.quizapp.dto;

import com.quizapp.model.RoomStatus;

import java.time.Instant;
import java.util.List;

public class FiveOhOneOnlineStateDto {
    private String roomCode;
    private RoomStatus status;
    private Long yourParticipantId;
    private List<FiveOhOneOnlinePlayerStateDto> players;
    private Long categoryId;
    private String categoryTitle;
    private String categoryDescription;
    private Instant categoryUpdatedAt;
    private List<Long> usedEntryIds;
    private Long currentTurnParticipantId;
    private Long windowReacherParticipantId;
    private Long winnerParticipantId;
    private boolean finished;
    private Integer bestAvailableScore; // null unless meaningful for whoever's turn it is
    private Integer checkoutCount; // null unless meaningful for whoever's turn it is
    private List<FiveOhOneThrowDto> throwHistory;

    public String getRoomCode() { return roomCode; }
    public void setRoomCode(String roomCode) { this.roomCode = roomCode; }
    public RoomStatus getStatus() { return status; }
    public void setStatus(RoomStatus status) { this.status = status; }
    public Long getYourParticipantId() { return yourParticipantId; }
    public void setYourParticipantId(Long yourParticipantId) { this.yourParticipantId = yourParticipantId; }
    public List<FiveOhOneOnlinePlayerStateDto> getPlayers() { return players; }
    public void setPlayers(List<FiveOhOneOnlinePlayerStateDto> players) { this.players = players; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public String getCategoryTitle() { return categoryTitle; }
    public void setCategoryTitle(String categoryTitle) { this.categoryTitle = categoryTitle; }
    public String getCategoryDescription() { return categoryDescription; }
    public void setCategoryDescription(String categoryDescription) { this.categoryDescription = categoryDescription; }
    public Instant getCategoryUpdatedAt() { return categoryUpdatedAt; }
    public void setCategoryUpdatedAt(Instant categoryUpdatedAt) { this.categoryUpdatedAt = categoryUpdatedAt; }
    public List<Long> getUsedEntryIds() { return usedEntryIds; }
    public void setUsedEntryIds(List<Long> usedEntryIds) { this.usedEntryIds = usedEntryIds; }
    public Long getCurrentTurnParticipantId() { return currentTurnParticipantId; }
    public void setCurrentTurnParticipantId(Long currentTurnParticipantId) { this.currentTurnParticipantId = currentTurnParticipantId; }
    public Long getWindowReacherParticipantId() { return windowReacherParticipantId; }
    public void setWindowReacherParticipantId(Long windowReacherParticipantId) { this.windowReacherParticipantId = windowReacherParticipantId; }
    public Long getWinnerParticipantId() { return winnerParticipantId; }
    public void setWinnerParticipantId(Long winnerParticipantId) { this.winnerParticipantId = winnerParticipantId; }
    public boolean isFinished() { return finished; }
    public void setFinished(boolean finished) { this.finished = finished; }
    public Integer getBestAvailableScore() { return bestAvailableScore; }
    public void setBestAvailableScore(Integer bestAvailableScore) { this.bestAvailableScore = bestAvailableScore; }
    public Integer getCheckoutCount() { return checkoutCount; }
    public void setCheckoutCount(Integer checkoutCount) { this.checkoutCount = checkoutCount; }
    public List<FiveOhOneThrowDto> getThrowHistory() { return throwHistory; }
    public void setThrowHistory(List<FiveOhOneThrowDto> throwHistory) { this.throwHistory = throwHistory; }
}
