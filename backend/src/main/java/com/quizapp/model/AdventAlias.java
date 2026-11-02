package com.quizapp.model;

import jakarta.persistence.*;

/**
 * The game master's correction of how one player answer is read on one door: "Bergen by" counts as the hidden answer
 * "Bergen" (answerIndex 0-3), or "Oslo" does NOT count even though it looked like a match (answerIndex -1). The text is
 * the normalised form (see AdventScoring.normalize), so every spelling that normalises to it is covered at once.
 * No row = the automatic reading (an exact match, ignoring case, accents and punctuation).
 */
@Entity
@Table(name = "advent_aliases",
        uniqueConstraints = @UniqueConstraint(name = "uq_advent_aliases_day_text", columnNames = {"advent_day_id", "normalized_text"}))
public class AdventAlias {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "advent_day_id", nullable = false)
    private Long dayId;

    @Column(name = "normalized_text", nullable = false, length = 200)
    private String normalizedText;

    @Column(name = "answer_index", nullable = false)
    private int answerIndex;

    public Long getId() { return id; }
    public Long getDayId() { return dayId; }
    public void setDayId(Long dayId) { this.dayId = dayId; }
    public String getNormalizedText() { return normalizedText; }
    public void setNormalizedText(String normalizedText) { this.normalizedText = normalizedText; }
    public int getAnswerIndex() { return answerIndex; }
    public void setAnswerIndex(int answerIndex) { this.answerIndex = answerIndex; }
}
