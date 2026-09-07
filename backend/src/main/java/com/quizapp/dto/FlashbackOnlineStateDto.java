package com.quizapp.dto;

import com.quizapp.model.RoomStatus;

import java.util.List;

public class FlashbackOnlineStateDto {
    private String roomCode;
    private RoomStatus status;
    private Long yourParticipantId;
    private int currentRoundIndex;
    private int totalRounds;
    private int hintIndex;
    // Only the hints revealed so far - never the target year itself until the
    // round actually resolves, unlike Bullseye's entries. There's no
    // client-side lookup that needs it early (a plain year guess, no
    // autocomplete), so there's no reason to trade away real secrecy here the
    // way Bullseye does for its answer-picker's search.
    private List<String> visibleHints;
    private Integer year; // null until roundRevealed
    private List<FlashbackOnlinePlayerDto> players;
    private Long currentTurnParticipantId;
    private List<FlashbackOnlineGuessDto> guesses;
    private boolean roundRevealed;
    private List<String> roundWinners;
    private int pointsAwarded;
    private boolean wasExactMatch;
    private boolean finished;

    public String getRoomCode() { return roomCode; }
    public void setRoomCode(String roomCode) { this.roomCode = roomCode; }
    public RoomStatus getStatus() { return status; }
    public void setStatus(RoomStatus status) { this.status = status; }
    public Long getYourParticipantId() { return yourParticipantId; }
    public void setYourParticipantId(Long yourParticipantId) { this.yourParticipantId = yourParticipantId; }
    public int getCurrentRoundIndex() { return currentRoundIndex; }
    public void setCurrentRoundIndex(int currentRoundIndex) { this.currentRoundIndex = currentRoundIndex; }
    public int getTotalRounds() { return totalRounds; }
    public void setTotalRounds(int totalRounds) { this.totalRounds = totalRounds; }
    public int getHintIndex() { return hintIndex; }
    public void setHintIndex(int hintIndex) { this.hintIndex = hintIndex; }
    public List<String> getVisibleHints() { return visibleHints; }
    public void setVisibleHints(List<String> visibleHints) { this.visibleHints = visibleHints; }
    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }
    public List<FlashbackOnlinePlayerDto> getPlayers() { return players; }
    public void setPlayers(List<FlashbackOnlinePlayerDto> players) { this.players = players; }
    public Long getCurrentTurnParticipantId() { return currentTurnParticipantId; }
    public void setCurrentTurnParticipantId(Long currentTurnParticipantId) { this.currentTurnParticipantId = currentTurnParticipantId; }
    public List<FlashbackOnlineGuessDto> getGuesses() { return guesses; }
    public void setGuesses(List<FlashbackOnlineGuessDto> guesses) { this.guesses = guesses; }
    public boolean isRoundRevealed() { return roundRevealed; }
    public void setRoundRevealed(boolean roundRevealed) { this.roundRevealed = roundRevealed; }
    public List<String> getRoundWinners() { return roundWinners; }
    public void setRoundWinners(List<String> roundWinners) { this.roundWinners = roundWinners; }
    public int getPointsAwarded() { return pointsAwarded; }
    public void setPointsAwarded(int pointsAwarded) { this.pointsAwarded = pointsAwarded; }
    public boolean isWasExactMatch() { return wasExactMatch; }
    public void setWasExactMatch(boolean wasExactMatch) { this.wasExactMatch = wasExactMatch; }
    public boolean isFinished() { return finished; }
    public void setFinished(boolean finished) { this.finished = finished; }
}
