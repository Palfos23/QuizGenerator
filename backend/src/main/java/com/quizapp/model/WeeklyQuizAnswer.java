package com.quizapp.model;

import jakarta.persistence.*;

@Entity
@Table(name = "weekly_quiz_answers")
public class WeeklyQuizAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attempt_id", nullable = false)
    private WeeklyQuizAttempt attempt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(name = "answer_text", nullable = false, length = 1000)
    private String answerText;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WeeklyQuizAnswerVerdict verdict = WeeklyQuizAnswerVerdict.PENDING;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public WeeklyQuizAttempt getAttempt() {
        return attempt;
    }

    public void setAttempt(WeeklyQuizAttempt attempt) {
        this.attempt = attempt;
    }

    public Question getQuestion() {
        return question;
    }

    public void setQuestion(Question question) {
        this.question = question;
    }

    public String getAnswerText() {
        return answerText;
    }

    public void setAnswerText(String answerText) {
        this.answerText = answerText;
    }

    public WeeklyQuizAnswerVerdict getVerdict() {
        return verdict;
    }

    public void setVerdict(WeeklyQuizAnswerVerdict verdict) {
        this.verdict = verdict;
    }
}
