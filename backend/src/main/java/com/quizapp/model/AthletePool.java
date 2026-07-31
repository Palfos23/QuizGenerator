package com.quizapp.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "athlete_pools")
public class AthletePool {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String name;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Sport sport;

    // Just a set of athletes - no per-membership data at all. A grid's own
    // candidates carry hint/correct-answer info specific to that grid; a pool
    // is deliberately simpler than that, since it's meant to be reused as-is
    // across many different grids, each of which will assign its own hints.
    @ManyToMany
    @JoinTable(
            name = "athlete_pool_members",
            joinColumns = @JoinColumn(name = "pool_id"),
            inverseJoinColumns = @JoinColumn(name = "athlete_id"))
    private Set<Athlete> members = new HashSet<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Sport getSport() {
        return sport;
    }

    public void setSport(Sport sport) {
        this.sport = sport;
    }

    public Set<Athlete> getMembers() {
        return members;
    }

    public void setMembers(Set<Athlete> members) {
        // Mutates the existing managed collection in place, rather than
        // reassigning this field to a brand-new Set - reassigning means
        // Hibernate can't diff against what it was already tracking, so it
        // deletes every join-table row and re-inserts every row on every
        // save, regardless of how little actually changed. Same fix already
        // proven necessary for Grid's own candidates/entries collections.
        Set<Athlete> incoming = members != null ? members : new HashSet<>();
        this.members.removeIf(existing -> !incoming.contains(existing));
        for (Athlete a : incoming) {
            if (!this.members.contains(a)) {
                this.members.add(a);
            }
        }
    }
}
