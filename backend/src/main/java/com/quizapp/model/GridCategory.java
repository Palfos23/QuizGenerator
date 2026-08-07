package com.quizapp.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "grid_categories")
public class GridCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The category name IS the value stored on athletes/grids/pools/clubs -
    // there's no separate id-based foreign key linking them, matching how
    // Tension's own categories work (a plain string, validated at the
    // application layer against this list, not a database-level FK). This
    // keeps the migration from the old Sport enum minimal, since those
    // columns are already plain strings under the hood.
    @NotBlank
    @Column(nullable = false, unique = true)
    private String name;

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
}
