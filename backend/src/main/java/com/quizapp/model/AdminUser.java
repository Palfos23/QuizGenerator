package com.quizapp.model;

import jakarta.persistence.*;

/**
 * Deliberately a separate table from {@link AppUser}: there is no self-service
 * sign-up path for admins, no link to this in the regular UI, and no route
 * that promotes a regular Google-authenticated user into this table. Admin
 * accounts only ever come from the database seed or a DB admin inserting a
 * row directly.
 */
@Entity
@Table(name = "admin_users")
public class AdminUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }
}
