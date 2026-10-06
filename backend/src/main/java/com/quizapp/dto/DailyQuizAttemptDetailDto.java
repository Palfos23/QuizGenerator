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
    private String status; // SUBMITTED (still has answers waiting) / GRADED
    private Integer score; // only once GRADED - shown to the admin so a changed decision visibly moves it
    private int maxScore;
    private List<AnswerDto> answers;

    public DailyQuizAttemptDetailDto(Long attemptId, Long setId, String playerName, LocalDate quizDate,
                                     String status, Integer score, int maxScore, List<AnswerDto> answers) {
        this.attemptId = attemptId;
        this.setId = setId;
        this.playerName = playerName;
        this.quizDate = quizDate;
        this.status = status;
        this.score = score;
        this.maxScore = maxScore;
        this.answers = answers;
    }

    public String getStatus() {
        return status;
    }

    public Integer getScore() {
        return score;
    }

    public int getMaxScore() {
        return maxScore;
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
        private String photoUrl; // null for questions without a picture - the admin judging a logo answer needs to see it
        private boolean yearQuestion; // auto-graded (2 / 1 / 0 points) - never needs a decision
        private int points; // what a CORRECT verdict is worth
        private boolean reviewable; // an admin can (re)decide this one - false for auto-graded answers

        public AnswerDto(Long answerId, int questionNumber, String questionText, String submittedAnswer, String correctAnswer, String verdict,
                         String photoUrl, boolean yearQuestion, int points, boolean reviewable) {
            this.answerId = answerId;
            this.questionNumber = questionNumber;
            this.questionText = questionText;
            this.submittedAnswer = submittedAnswer;
            this.correctAnswer = correctAnswer;
            this.verdict = verdict;
            this.photoUrl = photoUrl;
            this.yearQuestion = yearQuestion;
            this.points = points;
            this.reviewable = reviewable;
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

        public String getPhotoUrl() {
            return photoUrl;
        }

        public boolean isYearQuestion() {
            return yearQuestion;
        }

        public int getPoints() {
            return points;
        }

        public boolean isReviewable() {
            return reviewable;
        }
    }
}
