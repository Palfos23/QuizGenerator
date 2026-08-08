package com.quizapp.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "questions")
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "question_id")
    private Long id;

    @NotBlank
    @Column(name = "question_text", nullable = false, length = 2000)
    private String questionText;

    @NotBlank
    @Column(name = "category", nullable = false)
    private String category;

    // 1 (easiest) to 10 (hardest).
    @NotNull
    @Min(1)
    @Max(10)
    @Column(name = "difficulty_level", nullable = false)
    private Integer difficultyLevel;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "language", nullable = false)
    private Language language = Language.EN;

    // The single correct answer to the question.
    @NotBlank
    @Column(name = "answer", nullable = false, length = 1000)
    private String answer;

    // True for answers that can go stale over time (e.g. "current Premier League top scorer"),
    // false for answers that are effectively permanent (e.g. "capital of Norway"). Lets admins
    // quickly find questions that are due a recheck.
    @Column(name = "could_change", nullable = false)
    private boolean couldChange = false;

    // Plain hosted image URL, not a file upload - same pattern as Athlete.photoUrl and
    // Club.logoUrl elsewhere in this app, since Render's filesystem is ephemeral and would
    // lose any uploaded file on the next redeploy. Optional - most questions have none.
    @Column(name = "photo_url")
    private String photoUrl;

    // Reusable tags spanning categories (e.g. "Lord of the Rings" on both a Literature
    // and a Movies question) - lets quiz generation pull questions by theme regardless
    // of category. Plain @ManyToMany, no owned child entity - same pattern already
    // proven for AthletePool.members, avoids any of the Hibernate collection-diffing
    // complexity that applies to Grid's own owned collections.
    @ManyToMany
    @JoinTable(
            name = "question_label_links",
            joinColumns = @JoinColumn(name = "question_id"),
            inverseJoinColumns = @JoinColumn(name = "label_id"))
    private java.util.Set<QuestionLabel> labels = new java.util.HashSet<>();

    public Question() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getQuestionText() {
        return questionText;
    }

    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Integer getDifficultyLevel() {
        return difficultyLevel;
    }

    public void setDifficultyLevel(Integer difficultyLevel) {
        this.difficultyLevel = difficultyLevel;
    }

    public Language getLanguage() {
        return language;
    }

    public void setLanguage(Language language) {
        this.language = language;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public boolean isCouldChange() {
        return couldChange;
    }

    public void setCouldChange(boolean couldChange) {
        this.couldChange = couldChange;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public java.util.Set<QuestionLabel> getLabels() {
        return labels;
    }

    public void setLabels(java.util.Set<QuestionLabel> labels) {
        // Mutates the existing managed collection in place, rather than
        // reassigning this field to a brand-new Set - same fix already
        // proven necessary for AthletePool.members and Grid's own
        // collections. Reassigning means Hibernate can't diff against what
        // it was already tracking, so it deletes every join-table row and
        // re-inserts every row on every save, regardless of how little
        // actually changed.
        java.util.Set<QuestionLabel> incoming = labels != null ? labels : new java.util.HashSet<>();
        this.labels.removeIf(existing -> !incoming.contains(existing));
        for (QuestionLabel l : incoming) {
            if (!this.labels.contains(l)) {
                this.labels.add(l);
            }
        }
    }
}
