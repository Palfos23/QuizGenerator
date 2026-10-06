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

    // Points this answer earns IF its verdict is CORRECT - 1 for an ordinary question, 1 or 2 for the
    // daily Year question (see DailyQuizScoring). The database-level default of 1 matters: this
    // column was added to a table that already held rows, and under ddl-auto=update a NOT NULL
    // column with no default can't be added to a populated table (it's what took production down
    // once before). Every existing CORRECT answer was worth exactly 1, so 1 is also the right value.
    @Column(name = "points", nullable = false, columnDefinition = "integer not null default 1")
    private int points = 1;

    public Long getId() {
        return id;
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
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
