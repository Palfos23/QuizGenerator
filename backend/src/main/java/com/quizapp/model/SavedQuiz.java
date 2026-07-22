package com.quizapp.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "saved_quizzes")
public class SavedQuiz {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private AppUser owner;

    @NotBlank
    @Column(nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Language language;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    // Ordered snapshot of the questions as the user finalized them (after any
    // reordering/discard-and-replace) - independent of the live Question rows,
    // so later admin edits or deletions don't change a quiz someone already saved.
    @OneToMany(mappedBy = "savedQuiz", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("orderIndex ASC")
    private List<SavedQuizQuestion> questions = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public AppUser getOwner() {
        return owner;
    }

    public void setOwner(AppUser owner) {
        this.owner = owner;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Language getLanguage() {
        return language;
    }

    public void setLanguage(Language language) {
        this.language = language;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public List<SavedQuizQuestion> getQuestions() {
        return questions;
    }

    public void setQuestions(List<SavedQuizQuestion> questions) {
        this.questions.clear();
        if (questions != null) {
            for (SavedQuizQuestion q : questions) {
                q.setSavedQuiz(this);
                this.questions.add(q);
            }
        }
    }
}
