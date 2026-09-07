package com.quizapp.dto;

import com.quizapp.model.RoomStatus;

import java.time.Instant;
import java.util.List;

public class BullseyeOnlineStateDto {
    private String roomCode;
    private RoomStatus status;
    private Long yourParticipantId;
    private int currentQuestionIndex;
    private int totalQuestions;
    private String sport;
    private Integer targetValue;
    private String statLabel;
    private Instant questionUpdatedAt;
    // No secrecy to protect (same trust model as pass-and-play's BullseyeGame.vue
    // and BullseyePlayService.getMultiplayerStartState) - shipped once per round,
    // not gated behind reveal, so the answer-picker's search works immediately.
    private List<BullseyeEntryViewDto> entries;
    private List<BullseyeOnlinePlayerDto> players;
    private Long currentTurnParticipantId;
    private List<BullseyeOnlineAnswerDto> answersSoFar;
    private boolean roundRevealed;
    private List<BullseyeOnlineResultDto> roundResults;
    private List<BullseyeOnlineTruthDto> bullseyeAnswers;
    private boolean finished;

    public String getRoomCode() { return roomCode; }
    public void setRoomCode(String roomCode) { this.roomCode = roomCode; }
    public RoomStatus getStatus() { return status; }
    public void setStatus(RoomStatus status) { this.status = status; }
    public Long getYourParticipantId() { return yourParticipantId; }
    public void setYourParticipantId(Long yourParticipantId) { this.yourParticipantId = yourParticipantId; }
    public int getCurrentQuestionIndex() { return currentQuestionIndex; }
    public void setCurrentQuestionIndex(int currentQuestionIndex) { this.currentQuestionIndex = currentQuestionIndex; }
    public int getTotalQuestions() { return totalQuestions; }
    public void setTotalQuestions(int totalQuestions) { this.totalQuestions = totalQuestions; }
    public String getSport() { return sport; }
    public void setSport(String sport) { this.sport = sport; }
    public Integer getTargetValue() { return targetValue; }
    public void setTargetValue(Integer targetValue) { this.targetValue = targetValue; }
    public String getStatLabel() { return statLabel; }
    public void setStatLabel(String statLabel) { this.statLabel = statLabel; }
    public Instant getQuestionUpdatedAt() { return questionUpdatedAt; }
    public void setQuestionUpdatedAt(Instant questionUpdatedAt) { this.questionUpdatedAt = questionUpdatedAt; }
    public List<BullseyeEntryViewDto> getEntries() { return entries; }
    public void setEntries(List<BullseyeEntryViewDto> entries) { this.entries = entries; }
    public List<BullseyeOnlinePlayerDto> getPlayers() { return players; }
    public void setPlayers(List<BullseyeOnlinePlayerDto> players) { this.players = players; }
    public Long getCurrentTurnParticipantId() { return currentTurnParticipantId; }
    public void setCurrentTurnParticipantId(Long currentTurnParticipantId) { this.currentTurnParticipantId = currentTurnParticipantId; }
    public List<BullseyeOnlineAnswerDto> getAnswersSoFar() { return answersSoFar; }
    public void setAnswersSoFar(List<BullseyeOnlineAnswerDto> answersSoFar) { this.answersSoFar = answersSoFar; }
    public boolean isRoundRevealed() { return roundRevealed; }
    public void setRoundRevealed(boolean roundRevealed) { this.roundRevealed = roundRevealed; }
    public List<BullseyeOnlineResultDto> getRoundResults() { return roundResults; }
    public void setRoundResults(List<BullseyeOnlineResultDto> roundResults) { this.roundResults = roundResults; }
    public List<BullseyeOnlineTruthDto> getBullseyeAnswers() { return bullseyeAnswers; }
    public void setBullseyeAnswers(List<BullseyeOnlineTruthDto> bullseyeAnswers) { this.bullseyeAnswers = bullseyeAnswers; }
    public boolean isFinished() { return finished; }
    public void setFinished(boolean finished) { this.finished = finished; }
}
