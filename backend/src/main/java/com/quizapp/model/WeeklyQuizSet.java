package com.quizapp.model;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

// Auto-generated, unlike Grid (which admins hand-curate) - the first request
// for "this week's quiz" that finds no row for the current week's Monday
// creates one by randomly drawing 15 questions from the bank (see
// WeeklyQuizService.getOrCreateCurrentSet). No admin authoring step.
@Entity
@Table(name = "weekly_quiz_sets", uniqueConstraints = @UniqueConstraint(columnNames = "week_start_date"))
public class WeeklyQuizSet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "week_start_date", nullable = false)
    private LocalDate weekStartDate;

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

    public LocalDate getWeekStartDate() {
        return weekStartDate;
    }

    public void setWeekStartDate(LocalDate weekStartDate) {
        this.weekStartDate = weekStartDate;
    }

    public List<Long> getQuestionIds() {
        return questionIds;
    }

    public void setQuestionIds(List<Long> questionIds) {
        this.questionIds = questionIds;
    }
}
