package com.quizapp.dto;

import java.util.List;

// Sent to the player - deliberately carries no correct-answer data (no
// map x/y/city, no tile correctOwner). See BdayResultDto for the reveal.
public class BdayQuizDto {
    private String mapImageUrl;
    private List<MapQuestionDto> mapQuestions;
    private List<TileQuestionDto> tileQuestions;

    public String getMapImageUrl() {
        return mapImageUrl;
    }

    public void setMapImageUrl(String mapImageUrl) {
        this.mapImageUrl = mapImageUrl;
    }

    public List<MapQuestionDto> getMapQuestions() {
        return mapQuestions;
    }

    public void setMapQuestions(List<MapQuestionDto> mapQuestions) {
        this.mapQuestions = mapQuestions;
    }

    public List<TileQuestionDto> getTileQuestions() {
        return tileQuestions;
    }

    public void setTileQuestions(List<TileQuestionDto> tileQuestions) {
        this.tileQuestions = tileQuestions;
    }

    public static class MapQuestionDto {
        private String id;
        private String prompt;

        public MapQuestionDto() {
        }

        public MapQuestionDto(String id, String prompt) {
            this.id = id;
            this.prompt = prompt;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getPrompt() {
            return prompt;
        }

        public void setPrompt(String prompt) {
            this.prompt = prompt;
        }
    }

    public static class TileDto {
        private String id;
        private String label;

        public TileDto() {
        }

        public TileDto(String id, String label) {
            this.id = id;
            this.label = label;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }
    }

    public static class TileQuestionDto {
        private String id;
        private String prompt;
        private String personALabel;
        private String personBLabel;
        private List<TileDto> tiles;

        public TileQuestionDto() {
        }

        public TileQuestionDto(String id, String prompt, String personALabel, String personBLabel, List<TileDto> tiles) {
            this.id = id;
            this.prompt = prompt;
            this.personALabel = personALabel;
            this.personBLabel = personBLabel;
            this.tiles = tiles;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getPrompt() {
            return prompt;
        }

        public void setPrompt(String prompt) {
            this.prompt = prompt;
        }

        public String getPersonALabel() {
            return personALabel;
        }

        public void setPersonALabel(String personALabel) {
            this.personALabel = personALabel;
        }

        public String getPersonBLabel() {
            return personBLabel;
        }

        public void setPersonBLabel(String personBLabel) {
            this.personBLabel = personBLabel;
        }

        public List<TileDto> getTiles() {
            return tiles;
        }

        public void setTiles(List<TileDto> tiles) {
            this.tiles = tiles;
        }
    }
}
