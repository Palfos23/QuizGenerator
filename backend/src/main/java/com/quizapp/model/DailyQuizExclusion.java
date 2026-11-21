package com.quizapp.model;

import jakarta.persistence.*;

import java.time.LocalDate;

/**
 * "Not this type of question in that day's quiz" - set by an admin while previewing the next day's quiz (e.g.
 * no NFL questions tomorrow). A type is either a question category or a question label; the value is stored
 * as typed and compared case-insensitively. Only matters until its day has passed, so it's pruned soon after.
 */
@Entity
@Table(name = "daily_quiz_exclusions",
        uniqueConstraints = @UniqueConstraint(columnNames = {"quiz_date", "kind", "type_value"}),
        indexes = @Index(name = "idx_dq_exclusions_date", columnList = "quiz_date"))
public class DailyQuizExclusion {

    public static final String CATEGORY = "CATEGORY";
    public static final String LABEL = "LABEL";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "quiz_date", nullable = false)
    private LocalDate quizDate;

    // CATEGORY or LABEL
    @Column(name = "kind", nullable = false, length = 10)
    private String kind;

    @Column(name = "type_value", nullable = false, length = 200)
    private String value;

    public DailyQuizExclusion() {
    }

    public DailyQuizExclusion(LocalDate quizDate, String kind, String value) {
        this.quizDate = quizDate;
        this.kind = kind;
        this.value = value;
    }

    public Long getId() { return id; }
    public LocalDate getQuizDate() { return quizDate; }
    public String getKind() { return kind; }
    public String getValue() { return value; }

    /** True when this exclusion covers the question (its category, or one of its labels). */
    public boolean matches(Question q) {
        String wanted = value.trim();
        if (CATEGORY.equals(kind)) {
            return q.getCategory() != null && q.getCategory().trim().equalsIgnoreCase(wanted);
        }
        return q.getLabels().stream().anyMatch(l -> l.getName() != null && l.getName().trim().equalsIgnoreCase(wanted));
    }
}
