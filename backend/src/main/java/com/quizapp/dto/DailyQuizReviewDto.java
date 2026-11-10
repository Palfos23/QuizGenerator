package com.quizapp.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** The admin's per-answer review of the daily quiz and the players' complaints. */
public final class DailyQuizReviewDto {

    private DailyQuizReviewDto() {}

    /**
     * One distinct answer to one question, with how many players wrote it. rule: the decision remembered for it (true
     * correct, false wrong, null none). likelyTypo: it looks like a misspelling of the stored answer - a hint only.
     */
    public record Group(String text, String key, int count, int pendingCount, int correctCount, int incorrectCount,
                        boolean likelyTypo, Boolean rule) {}

    public record QuestionGroups(Long questionId, int number, String questionText, String correctAnswer, List<Group> groups) {}

    public record Day(Long setId, LocalDate quizDate, int pendingCount, List<QuestionGroups> questions) {}

    public record AnswerGroups(List<Day> days, int pendingTotal, int likelyTypoTotal) {}

    public record DecideGroupRequest(Long questionId, String text, boolean correct, boolean remember) {}

    public record AcceptTyposRequest(Long setId) {}

    /** How many answers a bulk decision changed. */
    public record Changed(int answers) {}

    public record ComplaintRequest(String comment) {}

    public record Complaint(Long id, Instant createdAt, String playerName, LocalDate quizDate, Long questionId, String questionText,
                            String correctAnswer, String playerAnswer, String comment, String decidedBy) {}

    public record AcceptComplaintRequest(boolean remember) {}

    public record RejectComplaintRequest(String note) {}
}
