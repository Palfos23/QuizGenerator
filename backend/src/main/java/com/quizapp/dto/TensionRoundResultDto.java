package com.quizapp.dto;

public class TensionRoundResultDto {
    private Long participantId;
    private String name;
    private String answerText;
    private int score;
    private boolean matchedTension;

    public TensionRoundResultDto(Long participantId, String name, String answerText, int score, boolean matchedTension) {
        this.participantId = participantId;
        this.name = name;
        this.answerText = answerText;
        this.score = score;
        this.matchedTension = matchedTension;
    }

    public Long getParticipantId() { return participantId; }
    public String getName() { return name; }
    public String getAnswerText() { return answerText; }
    public int getScore() { return score; }
    public boolean isMatchedTension() { return matchedTension; }
}
