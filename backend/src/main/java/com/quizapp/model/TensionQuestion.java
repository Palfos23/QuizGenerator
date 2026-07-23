package com.quizapp.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.annotations.SQLRestriction;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tension_questions")
public class TensionQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, length = 500)
    private String title;

    // Free text, e.g. "Geography", "Sport" - used to filter which questions a game draws from.
    @Column(name = "main_category")
    private String mainCategory;

    // Name of the TensionCategory whose word list powers the answer-box autocomplete
    // for this question - a broader suggestion pool, not necessarily all correct answers.
    @Column(name = "answers_category")
    private String answersCategory;

    // The safe list - correct answers ranked 1..10. Position 10 (the least obvious
    // still-safe answer) is worth the most; position 1 is worth the least.
    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @SQLRestriction("tension = false")
    private List<TensionAnswerEntry> safeAnswers = new ArrayList<>();

    // The tension list - answers ranked just past the safe cutoff. Guessing one of
    // these costs a flat -5, regardless of its own rank.
    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @SQLRestriction("tension = true")
    private List<TensionAnswerEntry> tensionAnswers = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMainCategory() {
        return mainCategory;
    }

    public void setMainCategory(String mainCategory) {
        this.mainCategory = mainCategory;
    }

    public String getAnswersCategory() {
        return answersCategory;
    }

    public void setAnswersCategory(String answersCategory) {
        this.answersCategory = answersCategory;
    }

    public List<TensionAnswerEntry> getSafeAnswers() {
        return safeAnswers;
    }

    public void setSafeAnswers(List<TensionAnswerEntry> safeAnswers) {
        this.safeAnswers.clear();
        if (safeAnswers != null) {
            for (TensionAnswerEntry e : safeAnswers) {
                e.setQuestion(this);
                e.setTension(false);
                this.safeAnswers.add(e);
            }
        }
    }

    public List<TensionAnswerEntry> getTensionAnswers() {
        return tensionAnswers;
    }

    public void setTensionAnswers(List<TensionAnswerEntry> tensionAnswers) {
        this.tensionAnswers.clear();
        if (tensionAnswers != null) {
            for (TensionAnswerEntry e : tensionAnswers) {
                e.setQuestion(this);
                e.setTension(true);
                this.tensionAnswers.add(e);
            }
        }
    }
}
