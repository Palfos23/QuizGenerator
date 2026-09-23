package com.quizapp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "five_oh_one_entries")
public class FiveOhOneEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    @JsonIgnore
    private FiveOhOneCategory category;

    @NotBlank
    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private int value;

    // Nullable, unlike BullseyeEntry.athlete - existing categories predate
    // this link and plenty of entries are still plain free text with no
    // matching Subject. Set once an entry is matched (CSV import) or created
    // (AddSubjectsModal) against a real Athlete, so FiveOhOneCategoryService's
    // "entire category" pool expansion can tell this entry apart from a
    // subject it hasn't listed yet, instead of only being able to compare by
    // name.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "athlete_id")
    private Athlete athlete;

    public Athlete getAthlete() {
        return athlete;
    }

    public void setAthlete(Athlete athlete) {
        this.athlete = athlete;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public FiveOhOneCategory getCategory() {
        return category;
    }

    public void setCategory(FiveOhOneCategory category) {
        this.category = category;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }
}
