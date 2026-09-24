package com.quizapp.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public class BdaySubmitRequest {
    @Valid
    private List<MapAnswer> mapAnswers;
    @Valid
    private List<TileAnswer> tileAnswers;

    public List<MapAnswer> getMapAnswers() {
        return mapAnswers;
    }

    public void setMapAnswers(List<MapAnswer> mapAnswers) {
        this.mapAnswers = mapAnswers;
    }

    public List<TileAnswer> getTileAnswers() {
        return tileAnswers;
    }

    public void setTileAnswers(List<TileAnswer> tileAnswers) {
        this.tileAnswers = tileAnswers;
    }

    public static class MapAnswer {
        @NotBlank
        private String questionId;
        private double x;
        private double y;

        public String getQuestionId() {
            return questionId;
        }

        public void setQuestionId(String questionId) {
            this.questionId = questionId;
        }

        public double getX() {
            return x;
        }

        public void setX(double x) {
            this.x = x;
        }

        public double getY() {
            return y;
        }

        public void setY(double y) {
            this.y = y;
        }
    }

    public static class TileAnswer {
        @NotBlank
        private String tileId;
        @NotBlank
        private String chosenOwner;

        public String getTileId() {
            return tileId;
        }

        public void setTileId(String tileId) {
            this.tileId = tileId;
        }

        public String getChosenOwner() {
            return chosenOwner;
        }

        public void setChosenOwner(String chosenOwner) {
            this.chosenOwner = chosenOwner;
        }
    }
}
