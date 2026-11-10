package com.quizapp.model;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * A player disagreeing with how one of their daily quiz answers was graded ("be om ny vurdering"). One per answer. It
 * lives and dies with the answer, so it is deleted with the quiz after 7 days.
 */
@Entity
@Table(name = "daily_quiz_complaints",
        uniqueConstraints = @UniqueConstraint(name = "uq_dq_complaint_answer", columnNames = {"answer_id"}))
public class DailyQuizComplaint {

    public enum Status { OPEN, ACCEPTED, REJECTED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "answer_id", nullable = false)
    private DailyQuizAnswer answer;

    @Column(length = 500)
    private String comment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.OPEN;

    // What the admin wrote back when turning the complaint down (shown to the player).
    @Column(name = "admin_note", length = 300)
    private String adminNote;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    public Long getId() { return id; }
    public DailyQuizAnswer getAnswer() { return answer; }
    public void setAnswer(DailyQuizAnswer answer) { this.answer = answer; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public String getAdminNote() { return adminNote; }
    public void setAdminNote(String adminNote) { this.adminNote = adminNote; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(Instant resolvedAt) { this.resolvedAt = resolvedAt; }
}
