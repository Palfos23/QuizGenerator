package com.quizapp.model;

import jakarta.persistence.*;

// Table name kept as "weekly_quiz_answers" - see DailyQuizSet's class
// comment for why (real production data already there).
@Entity
@Table(name = "weekly_quiz_answers")
public class DailyQuizAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attempt_id", nullable = false)
    private DailyQuizAttempt attempt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(name = "answer_text", nullable = false, length = 1000)
    private String answerText;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DailyQuizAnswerVerdict verdict = DailyQuizAnswerVerdict.PENDING;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public DailyQuizAttempt getAttempt() {
        return attempt;
    }

    public void setAttempt(DailyQuizAttempt attempt) {
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

    public DailyQuizAnswerVerdict getVerdict() {
        return verdict;
    }

    public void setVerdict(DailyQuizAnswerVerdict verdict) {
        this.verdict = verdict;
    }
}
