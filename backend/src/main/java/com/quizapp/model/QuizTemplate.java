package com.quizapp.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "quiz_templates")
public class QuizTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Language language;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    // Same snapshot approach as SavedQuiz - independent of the live Question rows,
    // so later admin edits to the shared bank don't change a template that's
    // already been copied into someone's My Quizzes.
    @OneToMany(mappedBy = "template", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("orderIndex ASC")
    private List<QuizTemplateQuestion> questions = new ArrayList<>();

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

    public List<QuizTemplateQuestion> getQuestions() {
        return questions;
    }

    public void setQuestions(List<QuizTemplateQuestion> questions) {
        this.questions.clear();
        if (questions != null) {
            for (QuizTemplateQuestion q : questions) {
                q.setTemplate(this);
                this.questions.add(q);
            }
        }
    }
}
