package com.quizapp.model;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * One door of the advent calendar: a category ("countries with a T in the name"), the kind of door, and the game
 * master's hidden answers. Written by an admin; the hidden answers are never sent to a player before the day closes.
 *
 * The date is derived (door N of season year Y opens on 1+N-1 December Y), so only the year and the number are stored.
 */
@Entity
@Table(name = "advent_days",
        uniqueConstraints = @UniqueConstraint(name = "uq_advent_days_season_day", columnNames = {"season_year", "day_number"}))
public class AdventDay {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "season_year", nullable = false)
    private int seasonYear;

    @Column(name = "day_number", nullable = false)
    private int dayNumber;

    @Column(length = 200)
    private String category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AdventType type = AdventType.NORMAL;

    // The hidden answers, one per line (a handful of short strings - not worth a collection table).
    @Column(name = "answers_text", length = 1000)
    private String answersText;

    // Upside-down doors only: other answers that fit the category, one per line (the hidden answers fit too). A player's
    // answer must fit the category for the 5 points - see AdventScoring.fits. Nullable: it is a column added after launch.
    @Column(name = "valid_answers_text", length = 4000)
    private String validAnswersText;

    // Which of the hidden answers is the wildcard (0-3), only for WILDCARD doors.
    @Column(name = "wildcard_index")
    private Integer wildcardIndex;

    // The game master has gone through the players' answers (see AdventAdminService) - until then results are "preliminary".
    @Column(nullable = false)
    private boolean approved = false;

    public List<String> getAnswers() {
        if (answersText == null || answersText.isBlank()) return new ArrayList<>();
        return new ArrayList<>(Arrays.asList(answersText.split("\n")));
    }

    public void setAnswers(List<String> answers) {
        this.answersText = String.join("\n", answers);
    }

    public List<String> getValidAnswers() {
        if (validAnswersText == null || validAnswersText.isBlank()) return new ArrayList<>();
        return new ArrayList<>(Arrays.asList(validAnswersText.split("\n")));
    }

    public void setValidAnswers(List<String> answers) {
        this.validAnswersText = answers.isEmpty() ? null : String.join("\n", answers);
    }

    /** Ready to be played: it has a category and all the hidden answers (and a wildcard where one is needed). */
    public boolean isReady() {
        if (category == null || category.isBlank()) return false;
        List<String> answers = getAnswers();
        if (answers.size() != type.answersRequired() || answers.stream().anyMatch(String::isBlank)) return false;
        return type != AdventType.WILDCARD || (wildcardIndex != null && wildcardIndex >= 0 && wildcardIndex < answers.size());
    }

    public Long getId() { return id; }
    public int getSeasonYear() { return seasonYear; }
    public void setSeasonYear(int seasonYear) { this.seasonYear = seasonYear; }
    public int getDayNumber() { return dayNumber; }
    public void setDayNumber(int dayNumber) { this.dayNumber = dayNumber; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public AdventType getType() { return type; }
    public void setType(AdventType type) { this.type = type; }
    public Integer getWildcardIndex() { return wildcardIndex; }
    public void setWildcardIndex(Integer wildcardIndex) { this.wildcardIndex = wildcardIndex; }
    public boolean isApproved() { return approved; }
    public void setApproved(boolean approved) { this.approved = approved; }
}
