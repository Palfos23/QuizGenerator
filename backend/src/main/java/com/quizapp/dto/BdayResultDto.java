package com.quizapp.dto;

import java.util.List;

public class BdayResultDto {
    private int score;
    private int maxScore;
    private List<MapResult> mapResults;
    private List<TileResult> tileResults;

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public int getMaxScore() {
        return maxScore;
    }

    public void setMaxScore(int maxScore) {
        this.maxScore = maxScore;
    }

    public List<MapResult> getMapResults() {
        return mapResults;
    }

    public void setMapResults(List<MapResult> mapResults) {
        this.mapResults = mapResults;
    }

    public List<TileResult> getTileResults() {
        return tileResults;
    }

    public void setTileResults(List<TileResult> tileResults) {
        this.tileResults = tileResults;
    }

    public static class MapResult {
        private String questionId;
        private boolean correct;
        private String correctCityName;
        private int pointsEarned;

        public MapResult() {
        }

        public MapResult(String questionId, boolean correct, String correctCityName, int pointsEarned) {
            this.questionId = questionId;
            this.correct = correct;
            this.correctCityName = correctCityName;
            this.pointsEarned = pointsEarned;
        }

        public String getQuestionId() {
            return questionId;
        }

        public void setQuestionId(String questionId) {
            this.questionId = questionId;
        }

        public boolean isCorrect() {
            return correct;
        }

        public void setCorrect(boolean correct) {
            this.correct = correct;
        }

        public String getCorrectCityName() {
            return correctCityName;
        }

        public void setCorrectCityName(String correctCityName) {
            this.correctCityName = correctCityName;
        }

        public int getPointsEarned() {
            return pointsEarned;
        }

        public void setPointsEarned(int pointsEarned) {
            this.pointsEarned = pointsEarned;
        }
    }

    public static class TileResult {
        private String tileId;
        private boolean correct;
        private String correctOwner;

        public TileResult() {
        }

        public TileResult(String tileId, boolean correct, String correctOwner) {
            this.tileId = tileId;
            this.correct = correct;
            this.correctOwner = correctOwner;
        }

        public String getTileId() {
            return tileId;
        }

        public void setTileId(String tileId) {
            this.tileId = tileId;
        }

        public boolean isCorrect() {
            return correct;
        }

        public void setCorrect(boolean correct) {
            this.correct = correct;
        }

        public String getCorrectOwner() {
            return correctOwner;
        }

        public void setCorrectOwner(String correctOwner) {
            this.correctOwner = correctOwner;
        }
    }
}
