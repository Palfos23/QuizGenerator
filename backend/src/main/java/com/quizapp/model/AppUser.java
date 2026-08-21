package com.quizapp.model;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "app_users")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    private String name;

    // Google's "sub" claim - stable unique id for the Google account
    @Column(name = "google_subject", unique = true)
    private String googleSubject;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    // Per-game play permissions - scaffolding for a future subscription model.
    // Default true so adding these columns is a no-op for every existing
    // account; an admin restricts a specific account from a specific game by
    // flipping its row to false directly in the database. columnDefinition
    // gives an explicit DB-level default so this applies cleanly via
    // ddl-auto=update to an already-populated table, same reasoning as
    // Grid.entireCategoryPool.
    @Column(name = "can_play_tension", nullable = false, columnDefinition = "boolean default true")
    private boolean canPlayTension = true;

    @Column(name = "can_play_grid_battle", nullable = false, columnDefinition = "boolean default true")
    private boolean canPlayGridBattle = true;

    @Column(name = "can_play_five_oh_one", nullable = false, columnDefinition = "boolean default true")
    private boolean canPlayFiveOhOne = true;

    @Column(name = "can_play_imposter", nullable = false, columnDefinition = "boolean default true")
    private boolean canPlayImposter = true;

    @Column(name = "can_play_starting_xi_battle", nullable = false, columnDefinition = "boolean default true")
    private boolean canPlayStartingXiBattle = true;

    @Column(name = "can_play_bullseye", nullable = false, columnDefinition = "boolean default true")
    private boolean canPlayBullseye = true;

    public boolean isCanPlayTension() {
        return canPlayTension;
    }

    public void setCanPlayTension(boolean canPlayTension) {
        this.canPlayTension = canPlayTension;
    }

    public boolean isCanPlayGridBattle() {
        return canPlayGridBattle;
    }

    public void setCanPlayGridBattle(boolean canPlayGridBattle) {
        this.canPlayGridBattle = canPlayGridBattle;
    }

    public boolean isCanPlayFiveOhOne() {
        return canPlayFiveOhOne;
    }

    public void setCanPlayFiveOhOne(boolean canPlayFiveOhOne) {
        this.canPlayFiveOhOne = canPlayFiveOhOne;
    }

    public boolean isCanPlayImposter() {
        return canPlayImposter;
    }

    public void setCanPlayImposter(boolean canPlayImposter) {
        this.canPlayImposter = canPlayImposter;
    }

    public boolean isCanPlayStartingXiBattle() {
        return canPlayStartingXiBattle;
    }

    public void setCanPlayStartingXiBattle(boolean canPlayStartingXiBattle) {
        this.canPlayStartingXiBattle = canPlayStartingXiBattle;
    }

    public boolean isCanPlayBullseye() {
        return canPlayBullseye;
    }

    public void setCanPlayBullseye(boolean canPlayBullseye) {
        this.canPlayBullseye = canPlayBullseye;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGoogleSubject() {
        return googleSubject;
    }

    public void setGoogleSubject(String googleSubject) {
        this.googleSubject = googleSubject;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
