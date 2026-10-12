package com.quizapp.model;

import jakarta.persistence.*;

import java.time.LocalDate;

/**
 * A durable note that a question appeared in the daily quiz of a given day.
 *
 * Quizzes themselves are deleted when they're 7 days old, so "which questions has the daily quiz used
 * lately?" can't be answered from them for longer than that - this tiny log is what lets a question stay
 * out of the quiz for 30 days (see DailyQuizService#NO_REPEAT_DAYS). It holds ids only and is pruned
 * once the entries are too old to matter.
 */
@Entity
@Table(name = "daily_quiz_question_uses", indexes = @Index(name = "idx_dq_question_uses_date", columnList = "quiz_date"))
public class DailyQuizQuestionUse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // A plain id, not a relation: a question deleted from the bank later must not be blocked by (or drag
    // along) these rows.
    @Column(name = "question_id", nullable = false)
    private Long questionId;

    @Column(name = "quiz_date", nullable = false)
    private LocalDate quizDate;

    public DailyQuizQuestionUse() {
    }

    public DailyQuizQuestionUse(Long questionId, LocalDate quizDate) {
        this.questionId = questionId;
        this.quizDate = quizDate;
    }

    public Long getId() { return id; }
    public Long getQuestionId() { return questionId; }
    public LocalDate getQuizDate() { return quizDate; }
}
