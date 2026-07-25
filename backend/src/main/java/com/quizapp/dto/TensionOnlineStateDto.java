package com.quizapp.dto;

import com.quizapp.model.RoomStatus;

import java.util.List;

public class TensionOnlineStateDto {
    private String roomCode;
    private RoomStatus status;
    private Long yourParticipantId;
    private int currentQuestionIndex;
    private int totalQuestions;
    private String questionTitle;
    private String answersCategory;
    private int tensionAnswerCount;
    private List<TensionOnlinePlayerDto> players;
    private boolean roundRevealed;
    private List<TensionAnswerEntryDto> safeAnswers;
    private List<TensionAnswerEntryDto> tensionAnswers;
    private List<TensionRoundResultDto> roundResults;
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
    public String getQuestionTitle() { return questionTitle; }
    public void setQuestionTitle(String questionTitle) { this.questionTitle = questionTitle; }
    public String getAnswersCategory() { return answersCategory; }
    public void setAnswersCategory(String answersCategory) { this.answersCategory = answersCategory; }
    public int getTensionAnswerCount() { return tensionAnswerCount; }
    public void setTensionAnswerCount(int tensionAnswerCount) { this.tensionAnswerCount = tensionAnswerCount; }
    public List<TensionOnlinePlayerDto> getPlayers() { return players; }
    public void setPlayers(List<TensionOnlinePlayerDto> players) { this.players = players; }
    public boolean isRoundRevealed() { return roundRevealed; }
    public void setRoundRevealed(boolean roundRevealed) { this.roundRevealed = roundRevealed; }
    public List<TensionAnswerEntryDto> getSafeAnswers() { return safeAnswers; }
    public void setSafeAnswers(List<TensionAnswerEntryDto> safeAnswers) { this.safeAnswers = safeAnswers; }
    public List<TensionAnswerEntryDto> getTensionAnswers() { return tensionAnswers; }
    public void setTensionAnswers(List<TensionAnswerEntryDto> tensionAnswers) { this.tensionAnswers = tensionAnswers; }
    public List<TensionRoundResultDto> getRoundResults() { return roundResults; }
    public void setRoundResults(List<TensionRoundResultDto> roundResults) { this.roundResults = roundResults; }
    public boolean isFinished() { return finished; }
    public void setFinished(boolean finished) { this.finished = finished; }
}
