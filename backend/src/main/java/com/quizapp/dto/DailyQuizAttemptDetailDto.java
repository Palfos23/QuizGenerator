package com.quizapp.dto;

import java.time.LocalDate;
import java.util.List;

// Admin-facing view of one player's full attempt - unlike the player-facing
// DailyQuizResultDto, the correct answer is always shown (there's nothing to
// spoil for the admin), even for still-PENDING rows, since that's exactly
// what they're using to judge each one.
public class DailyQuizAttemptDetailDto {

    private Long attemptId;
    private Long setId;
    private String playerName;
    private LocalDate quizDate;
    private List<AnswerDto> answers;

    public DailyQuizAttemptDetailDto(Long attemptId, Long setId, String playerName, LocalDate quizDate, List<AnswerDto> answers) {
        this.attemptId = attemptId;
        this.setId = setId;
        this.playerName = playerName;
        this.quizDate = quizDate;
        this.answers = answers;
    }

    public Long getAttemptId() {
        return attemptId;
    }

    public Long getSetId() {
        return setId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public LocalDate getQuizDate() {
        return quizDate;
    }

    public List<AnswerDto> getAnswers() {
        return answers;
    }

    public static class AnswerDto {
        private Long answerId;
        private int questionNumber;
        private String questionText;
        private String submittedAnswer;
        private String correctAnswer;
        private String verdict; // PENDING / CORRECT / INCORRECT

        public AnswerDto(Long answerId, int questionNumber, String questionText, String submittedAnswer, String correctAnswer, String verdict) {
            this.answerId = answerId;
            this.questionNumber = questionNumber;
            this.questionText = questionText;
            this.submittedAnswer = submittedAnswer;
            this.correctAnswer = correctAnswer;
            this.verdict = verdict;
        }

        public Long getAnswerId() {
            return answerId;
        }

        public int getQuestionNumber() {
            return questionNumber;
        }

        public String getQuestionText() {
            return questionText;
        }

        public String getSubmittedAnswer() {
            return submittedAnswer;
        }

        public String getCorrectAnswer() {
            return correctAnswer;
        }

        public String getVerdict() {
            return verdict;
        }
    }
}
