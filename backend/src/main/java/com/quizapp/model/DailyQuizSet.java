package com.quizapp.model;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

// Auto-generated, unlike Grid (which admins hand-curate) - the first request
// for "today's quiz" that finds no row for today's date creates one by
// randomly drawing 15 questions from the bank (see
// DailyQuizService.getOrCreateCurrentSet). No admin authoring step. A
// scheduled job (DailyQuizService.deleteOldSets) removes anything older than
// DailyQuizService.RETENTION_DAYS.
//
// Table/column names deliberately kept as "weekly_quiz_*"/"week_start_date" -
// this app was called "Weekly Quiz" before switching to a daily cadence, and
// those tables already hold real production data. Renaming them would need
// ddl-auto=update to rename a table/column, which it can't do (it would just
// create new empty ones and orphan the old data) - see the
// no-default-not-null-column-ddl-update incident this same feature already
// caused once. Only the Java/API-facing names changed.
@Entity
@Table(name = "weekly_quiz_sets", uniqueConstraints = @UniqueConstraint(columnNames = "week_start_date"))
public class DailyQuizSet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "week_start_date", nullable = false)
    private LocalDate quizDate;

    // Order matters - this is the numbered list a player sees (question 1, 2, 3...).
    @ElementCollection
    @CollectionTable(name = "weekly_quiz_set_questions", joinColumns = @JoinColumn(name = "set_id"))
    @Column(name = "question_id")
    @OrderColumn(name = "seq_order")
    private List<Long> questionIds = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getQuizDate() {
        return quizDate;
    }

    public void setQuizDate(LocalDate quizDate) {
        this.quizDate = quizDate;
    }

    public List<Long> getQuestionIds() {
        return questionIds;
    }

    public void setQuestionIds(List<Long> questionIds) {
        this.questionIds = questionIds;
    }
}
