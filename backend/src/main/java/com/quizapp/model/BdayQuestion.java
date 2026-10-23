package com.quizapp.model;

import jakarta.persistence.*;

import java.time.Instant;

// One admin-written question of the birthday quiz (the map question is fixed in BdayQuestionCatalog; everything
// else lives here so the host can write and change it from the admin page - see BdayQuizService).
//   STATEMENT - a statement; the guest picks Pål / Erik / Begge / Ingen (correctOption)
//   PHOTO     - a photo; the guest picks who is on it, Pål / Erik (correctOption)
//   TEXT      - a question the guest types an answer to; acceptedAnswers lists what counts as right
@Entity
@Table(name = "bday_questions")
public class BdayQuestion {

    public enum Type { STATEMENT, PHOTO, TEXT }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Type type;

    @Column(nullable = false, length = 500)
    private String prompt;

    // PHOTO only - an uploaded picture's link (or any image link).
    @Column(length = 1000)
    private String photoUrl;

    // STATEMENT / PHOTO: PAL, ERIK, BEGGE or INGEN.
    @Column(length = 10)
    private String correctOption;

    // TEXT only: one accepted answer per line.
    @Column(length = 2000)
    private String acceptedAnswers;

    @Column(nullable = false)
    private int sortOrder = 0;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Type getType() { return type; }
    public void setType(Type type) { this.type = type; }
    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }
    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }
    public String getCorrectOption() { return correctOption; }
    public void setCorrectOption(String correctOption) { this.correctOption = correctOption; }
    public String getAcceptedAnswers() { return acceptedAnswers; }
    public void setAcceptedAnswers(String acceptedAnswers) { this.acceptedAnswers = acceptedAnswers; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
    public Instant getCreatedAt() { return createdAt; }
}
