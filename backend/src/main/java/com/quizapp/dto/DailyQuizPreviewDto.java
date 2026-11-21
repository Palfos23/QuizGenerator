package com.quizapp.dto;

import java.time.LocalDate;
import java.util.List;

/** The next day's daily quiz as an admin sees it before it goes live (see DailyQuizPreviewService). */
public record DailyQuizPreviewDto(
        LocalDate date,
        Long setId,
        List<Item> questions,
        List<Exclusion> exclusions,
        /** For each category in the quiz: how many of its questions were in this quiz and the days before it. */
        List<CategoryRow> categoryHistory,
        /** The days the counts in categoryHistory refer to, in order (the previewed day first). */
        List<LocalDate> historyDays,
        List<String> allCategories,
        List<String> allLabels) {

    public record Item(
            int position,
            Long id,
            String text,
            String answer,
            String category,
            List<String> labels,
            Integer difficulty,
            String photoUrl,
            /** LOGO, YEAR or NORMAL - the two fixed slots keep their kind when a question is swapped. */
            String slot,
            /** Still covered by an exclusion (only possible when no replacement could be found). */
            boolean excluded,
            /** When this question last appeared in a daily quiz within the no-repeat window, if it did. */
            LocalDate lastUsed) {}

    public record Exclusion(String kind, String value) {}

    public record CategoryRow(String category, List<Integer> counts) {}

    public record Candidate(Long id, String text, String answer, String category, List<String> labels, LocalDate lastUsed) {}

    public record ExclusionRequest(String kind, String value) {}

    public record ReplaceRequest(Long questionId) {}

    public record ChooseRequest(Long questionId, Long newQuestionId) {}
}
