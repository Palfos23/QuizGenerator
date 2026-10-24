package com.quizapp.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

// A team in the birthday night's music quiz. The host keeps the team's score; every member of the team gets that score
// added to their own total (see BdayStandingsService).
@Entity
@Table(name = "bday_teams")
public class BdayTeam {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private int musicScore = 0;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getMusicScore() { return musicScore; }
    public void setMusicScore(int musicScore) { this.musicScore = musicScore; }
}
