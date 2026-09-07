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

    // Null for a Google-only account. Set for an account created via email+password,
    // or a Google account that's since added a password too - either sign-in method
    // works once both are present, same as most apps that support multiple providers.
    @Column(name = "password_hash")
    private String passwordHash;

    // Forgot-password support: a SHA-256 hex digest of a single-use random token
    // (not the token itself - see AuthService#requestPasswordReset), plus its
    // expiry. Both null when no reset is pending. A fast deterministic hash
    // (not BCrypt) is used here on purpose, since it needs to be looked up by
    // exact match rather than verified one row at a time.
    @Column(name = "reset_token_hash")
    private String resetTokenHash;

    @Column(name = "reset_token_expires_at")
    private Instant resetTokenExpiresAt;

    // True for a Google account (Google already verified the email during
    // OAuth) and, at the DB level, for every pre-existing row when this column
    // was added - same "default true, an ALTER TABLE backfill" trick as the
    // canPlayX flags below, so nobody who already had a working account got
    // logged out by this. Only a freshly password-registered account starts
    // false (see AuthService#registerWithPassword) and has to prove it owns
    // the email before loginWithPassword will let it in.
    @Column(name = "email_verified", nullable = false, columnDefinition = "boolean default true")
    private boolean emailVerified = true;

    @Column(name = "verification_token_hash")
    private String verificationTokenHash;

    @Column(name = "verification_token_expires_at")
    private Instant verificationTokenExpiresAt;

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

    @Column(name = "can_play_penalty_shootout", nullable = false, columnDefinition = "boolean default true")
    private boolean canPlayPenaltyShootout = true;

    @Column(name = "can_play_flashback", nullable = false, columnDefinition = "boolean default true")
    private boolean canPlayFlashback = true;

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

    public boolean isCanPlayPenaltyShootout() {
        return canPlayPenaltyShootout;
    }

    public void setCanPlayPenaltyShootout(boolean canPlayPenaltyShootout) {
        this.canPlayPenaltyShootout = canPlayPenaltyShootout;
    }

    public boolean isCanPlayFlashback() {
        return canPlayFlashback;
    }

    public void setCanPlayFlashback(boolean canPlayFlashback) {
        this.canPlayFlashback = canPlayFlashback;
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

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getResetTokenHash() {
        return resetTokenHash;
    }

    public void setResetTokenHash(String resetTokenHash) {
        this.resetTokenHash = resetTokenHash;
    }

    public Instant getResetTokenExpiresAt() {
        return resetTokenExpiresAt;
    }

    public void setResetTokenExpiresAt(Instant resetTokenExpiresAt) {
        this.resetTokenExpiresAt = resetTokenExpiresAt;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public String getVerificationTokenHash() {
        return verificationTokenHash;
    }

    public void setVerificationTokenHash(String verificationTokenHash) {
        this.verificationTokenHash = verificationTokenHash;
    }

    public Instant getVerificationTokenExpiresAt() {
        return verificationTokenExpiresAt;
    }

    public void setVerificationTokenExpiresAt(Instant verificationTokenExpiresAt) {
        this.verificationTokenExpiresAt = verificationTokenExpiresAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
