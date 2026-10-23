package com.quizapp.dto;

import java.util.List;

/** What a guest sees when starting the quiz - never the correct answers. */
public class BdayQuizDto {

    private String mapImageUrl;
    private List<MapQuestionDto> mapQuestions;
    private List<QuestionDto> questions;
    private List<OptionDto> statementOptions;
    private List<OptionDto> photoOptions;

    public String getMapImageUrl() { return mapImageUrl; }
    public void setMapImageUrl(String mapImageUrl) { this.mapImageUrl = mapImageUrl; }
    public List<MapQuestionDto> getMapQuestions() { return mapQuestions; }
    public void setMapQuestions(List<MapQuestionDto> mapQuestions) { this.mapQuestions = mapQuestions; }
    public List<QuestionDto> getQuestions() { return questions; }
    public void setQuestions(List<QuestionDto> questions) { this.questions = questions; }
    public List<OptionDto> getStatementOptions() { return statementOptions; }
    public void setStatementOptions(List<OptionDto> statementOptions) { this.statementOptions = statementOptions; }
    public List<OptionDto> getPhotoOptions() { return photoOptions; }
    public void setPhotoOptions(List<OptionDto> photoOptions) { this.photoOptions = photoOptions; }

    public static class MapQuestionDto {
        private String id;
        private String prompt;

        public MapQuestionDto(String id, String prompt) {
            this.id = id;
            this.prompt = prompt;
        }

        public String getId() { return id; }
        public String getPrompt() { return prompt; }
    }

    /** type is STATEMENT, PHOTO or TEXT; photoUrl is only set for PHOTO. */
    public static class QuestionDto {
        private String id;
        private String type;
        private String prompt;
        private String photoUrl;

        public QuestionDto(String id, String type, String prompt, String photoUrl) {
            this.id = id;
            this.type = type;
            this.prompt = prompt;
            this.photoUrl = photoUrl;
        }

        public String getId() { return id; }
        public String getType() { return type; }
        public String getPrompt() { return prompt; }
        public String getPhotoUrl() { return photoUrl; }
    }

    public static class OptionDto {
        private String code;
        private String label;

        public OptionDto(String code, String label) {
            this.code = code;
            this.label = label;
        }

        public String getCode() { return code; }
        public String getLabel() { return label; }
    }
}
