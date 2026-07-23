package com.quizapp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "tension_answer_entries")
public class TensionAnswerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    @JsonIgnore
    private TensionQuestion question;

    // Position within its own group - safe answers are ranked 1..10 independently
    // from tension answers, which have their own separate 1..N ranking. Matches the
    // original app's two-separate-numbered-lists design.
    @Column(nullable = false)
    private int rank;

    @NotBlank
    @Column(nullable = false)
    private String text;

    @Column(nullable = false)
    private boolean tension;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public TensionQuestion getQuestion() {
        return question;
    }

    public void setQuestion(TensionQuestion question) {
        this.question = question;
    }

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public boolean isTension() {
        return tension;
    }

    public void setTension(boolean tension) {
        this.tension = tension;
    }
}
