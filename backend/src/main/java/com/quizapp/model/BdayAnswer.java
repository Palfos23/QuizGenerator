package com.quizapp.model;

import jakarta.persistence.*;

import java.time.Instant;

// One row per guest per *question* (not per tile) - see BdayQuizService for
// how responseJson is written/read. Kept purely for a record of what was
// submitted; scoring itself is always recomputed from BdayQuestionCatalog,
// never trusted from the client.
@Entity
@Table(name = "bday_answers")
public class BdayAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guest_id", nullable = false)
    private BdayGuest guest;

    @Column(nullable = false)
    private String questionId;

    // {"x":0.23,"y":0.85} for a map question, {"tile-a":"A","tile-b":"B"} for
    // a tile question - small enough that a plain varchar is fine, no need
    // for a TEXT/CLOB column.
    @Column(nullable = false, length = 2000)
    private String responseJson;

    @Column(nullable = false)
    private int pointsEarned;

    @Column(nullable = false)
    private int maxPoints;

    @Column(nullable = false)
    private Instant answeredAt = Instant.now();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public BdayGuest getGuest() {
        return guest;
    }

    public void setGuest(BdayGuest guest) {
        this.guest = guest;
    }

    public String getQuestionId() {
        return questionId;
    }

    public void setQuestionId(String questionId) {
        this.questionId = questionId;
    }

    public String getResponseJson() {
        return responseJson;
    }

    public void setResponseJson(String responseJson) {
        this.responseJson = responseJson;
    }

    public int getPointsEarned() {
        return pointsEarned;
    }

    public void setPointsEarned(int pointsEarned) {
        this.pointsEarned = pointsEarned;
    }

    public int getMaxPoints() {
        return maxPoints;
    }

    public void setMaxPoints(int maxPoints) {
        this.maxPoints = maxPoints;
    }

    public Instant getAnsweredAt() {
        return answeredAt;
    }

    public void setAnsweredAt(Instant answeredAt) {
        this.answeredAt = answeredAt;
    }
}
