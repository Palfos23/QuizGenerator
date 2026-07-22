package com.quizapp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "saved_quiz_questions")
public class SavedQuizQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "saved_quiz_id", nullable = false)
    @JsonIgnore
    private SavedQuiz savedQuiz;

    // preserves the position the user put this question in (after any reordering)
    @Column(name = "order_index", nullable = false)
    private int orderIndex;

    @NotBlank
    @Column(name = "question_text", nullable = false, length = 2000)
    private String questionText;

    @NotBlank
    @Column(nullable = false)
    private String category;

    @Column(name = "difficulty_level", nullable = false)
    private int difficultyLevel;

    @NotBlank
    @Column(nullable = false, length = 1000)
    private String answer;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public SavedQuiz getSavedQuiz() {
        return savedQuiz;
    }

    public void setSavedQuiz(SavedQuiz savedQuiz) {
        this.savedQuiz = savedQuiz;
    }

    public int getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(int orderIndex) {
        this.orderIndex = orderIndex;
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

    public int getDifficultyLevel() {
        return difficultyLevel;
    }

    public void setDifficultyLevel(int difficultyLevel) {
        this.difficultyLevel = difficultyLevel;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }
}
