package com.quizapp.dto;

import java.util.List;

// Doubles as both the "still under review" view (answers populated, score
// null) and the fully "graded" view (both populated) - see
// DailyQuizService.buildAnswersView. Score stays null until every answer has
// a final verdict, per the product decision that the whole score is withheld
// until nothing is left pending.
public class DailyQuizResultDto {

    private Integer score;
    private int maxScore;
    private List<AnswerResultDto> answers;

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public int getMaxScore() {
        return maxScore;
    }

    public void setMaxScore(int maxScore) {
        this.maxScore = maxScore;
    }

    public List<AnswerResultDto> getAnswers() {
        return answers;
    }

    public void setAnswers(List<AnswerResultDto> answers) {
        this.answers = answers;
    }

    public static class AnswerResultDto {
        private int questionNumber;
        private String questionText;
        private String yourAnswer;
        private String correctAnswer; // null while verdict is PENDING - not spoiled before it's resolved
        private String verdict; // PENDING / CORRECT / INCORRECT

        public AnswerResultDto() {
        }

        public AnswerResultDto(int questionNumber, String questionText, String yourAnswer, String correctAnswer, String verdict) {
            this.questionNumber = questionNumber;
            this.questionText = questionText;
            this.yourAnswer = yourAnswer;
            this.correctAnswer = correctAnswer;
            this.verdict = verdict;
        }

        public int getQuestionNumber() {
            return questionNumber;
        }

        public void setQuestionNumber(int questionNumber) {
            this.questionNumber = questionNumber;
        }

        public String getQuestionText() {
            return questionText;
        }

        public void setQuestionText(String questionText) {
            this.questionText = questionText;
        }

        public String getYourAnswer() {
            return yourAnswer;
        }

        public void setYourAnswer(String yourAnswer) {
            this.yourAnswer = yourAnswer;
        }

        public String getCorrectAnswer() {
            return correctAnswer;
        }

        public void setCorrectAnswer(String correctAnswer) {
            this.correctAnswer = correctAnswer;
        }

        public String getVerdict() {
            return verdict;
        }

        public void setVerdict(String verdict) {
            this.verdict = verdict;
        }
    }
}
