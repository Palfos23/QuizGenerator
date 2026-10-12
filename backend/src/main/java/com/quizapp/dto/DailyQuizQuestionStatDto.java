package com.quizapp.dto;

/**
 * How a daily quiz question went for the players who answered it: the share of the available points they
 * earned (so a question with partial credit, like the Year question, counts proportionally) and how many
 * answers that is based on. The expert and answers still waiting on an admin are left out.
 */
public class DailyQuizQuestionStatDto {

    private final Long questionId;
    private final int percentCorrect;
    private final int players;

    public DailyQuizQuestionStatDto(Long questionId, int percentCorrect, int players) {
        this.questionId = questionId;
        this.percentCorrect = percentCorrect;
        this.players = players;
    }

    public Long getQuestionId() { return questionId; }
    public int getPercentCorrect() { return percentCorrect; }
    public int getPlayers() { return players; }
}
