package com.quizapp.dto;

import java.util.List;

/** What a guest sees after submitting: the score, how the map pin did, and how many they got right per section. */
public class BdayResultDto {

    private int score;
    private int maxScore;
    private List<MapResult> mapResults;
    private List<SectionResult> sections;

    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }
    public int getMaxScore() { return maxScore; }
    public void setMaxScore(int maxScore) { this.maxScore = maxScore; }
    public List<MapResult> getMapResults() { return mapResults; }
    public void setMapResults(List<MapResult> mapResults) { this.mapResults = mapResults; }
    public List<SectionResult> getSections() { return sections; }
    public void setSections(List<SectionResult> sections) { this.sections = sections; }

    public static class MapResult {
        private String questionId;
        private boolean correct;
        private String correctCityName;
        private int points;
        private double distanceKm;

        public MapResult(String questionId, boolean correct, String correctCityName, int points, double distanceKm) {
            this.questionId = questionId;
            this.correct = correct;
            this.correctCityName = correctCityName;
            this.points = points;
            this.distanceKm = distanceKm;
        }

        public String getQuestionId() { return questionId; }
        public boolean isCorrect() { return correct; }
        public String getCorrectCityName() { return correctCityName; }
        public int getPoints() { return points; }
        public double getDistanceKm() { return distanceKm; }
    }

    /** "7 of 10 statements right" - only counts, never which ones, so a finished guest can't pass the answers on. */
    public static class SectionResult {
        private String type;
        private int correct;
        private int total;

        public SectionResult(String type, int correct, int total) {
            this.type = type;
            this.correct = correct;
            this.total = total;
        }

        public String getType() { return type; }
        public int getCorrect() { return correct; }
        public int getTotal() { return total; }
    }
}
