package com.quizapp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "bullseye_entries")
public class BullseyeEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    @JsonIgnore
    private BullseyeQuestion question;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "athlete_id", nullable = false)
    private Athlete athlete;

    // This athlete's real stat value for this question, e.g. 27 goals - always
    // required, same as GridEntry.hintValue on a ranked grid. Coverage for
    // "everyone else in the category, no value needed" comes from
    // BullseyeQuestion.entireCategoryPool instead of a null value here - see
    // BullseyePlayService.getMultiplayerStartState.
    @Column(name = "stat_value", nullable = false)
    private Integer statValue;

    // Display order in the admin editor and the reveal fallback ordering.
    @Column(name = "order_index", nullable = false)
    private int orderIndex;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public BullseyeQuestion getQuestion() {
        return question;
    }

    public void setQuestion(BullseyeQuestion question) {
        this.question = question;
    }

    public Athlete getAthlete() {
        return athlete;
    }

    public void setAthlete(Athlete athlete) {
        this.athlete = athlete;
    }

    public Integer getStatValue() {
        return statValue;
    }

    public void setStatValue(Integer statValue) {
        this.statValue = statValue;
    }

    public int getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(int orderIndex) {
        this.orderIndex = orderIndex;
    }
}
