package com.quizapp.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import java.util.List;

public class BdaySubmitRequest {

    @Valid
    private List<MapAnswer> mapAnswers;
    @Valid
    private List<Answer> answers;

    public List<MapAnswer> getMapAnswers() { return mapAnswers; }
    public void setMapAnswers(List<MapAnswer> mapAnswers) { this.mapAnswers = mapAnswers; }
    public List<Answer> getAnswers() { return answers; }
    public void setAnswers(List<Answer> answers) { this.answers = answers; }

    /** A pin on the map: x/y are fractions (0..1) of the image's width and height. */
    public static class MapAnswer {
        private String questionId;
        private double x;
        private double y;

        public String getQuestionId() { return questionId; }
        public void setQuestionId(String questionId) { this.questionId = questionId; }
        public double getX() { return x; }
        public void setX(double x) { this.x = x; }
        public double getY() { return y; }
        public void setY(double y) { this.y = y; }
    }

    /** An answer to a statement / photo / text question: an option code (PAL, ERIK, BEGGE, INGEN) or typed text. */
    public static class Answer {
        private String questionId;
        @Size(max = 300)
        private String value;

        public String getQuestionId() { return questionId; }
        public void setQuestionId(String questionId) { this.questionId = questionId; }
        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
    }
}
