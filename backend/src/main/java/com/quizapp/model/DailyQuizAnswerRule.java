package com.quizapp.model;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * An admin decision remembered for a question: this (normalised) answer counts as correct, or as wrong. When the same
 * answer turns up again - from another player, or the next time the question is used - it is graded straight away and
 * nobody has to look at it again. Keyed by the question's id with no foreign key, so it quietly stops mattering if the
 * question is deleted from the bank.
 */
@Entity
@Table(name = "daily_quiz_answer_rules",
        uniqueConstraints = @UniqueConstraint(name = "uq_dq_answer_rule_question_text", columnNames = {"question_id", "normalized_text"}))
public class DailyQuizAnswerRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "question_id", nullable = false)
    private Long questionId;

    @Column(name = "normalized_text", nullable = false, length = 200)
    private String normalizedText;

    @Column(nullable = false)
    private boolean correct;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public Long getQuestionId() { return questionId; }
    public void setQuestionId(Long questionId) { this.questionId = questionId; }
    public String getNormalizedText() { return normalizedText; }
    public void setNormalizedText(String normalizedText) { this.normalizedText = normalizedText; }
    public boolean isCorrect() { return correct; }
    public void setCorrect(boolean correct) { this.correct = correct; }
    public Instant getCreatedAt() { return createdAt; }
}
