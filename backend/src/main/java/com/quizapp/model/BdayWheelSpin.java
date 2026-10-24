package com.quizapp.model;

import jakarta.persistence.*;

import java.time.Instant;

// The one spin of the wheel each guest gets: what it landed on, as bonus points (can be negative) added to their total.
@Entity
@Table(name = "bday_wheel_spins")
public class BdayWheelSpin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "guest_id", nullable = false, unique = true)
    private BdayGuest guest;

    @Column(nullable = false)
    private int bonusPoints;

    // What the wheel landed on, e.g. "Dobbel opp" - just for the host's overview.
    @Column(length = 100)
    private String note;

    @Column(nullable = false)
    private Instant spunAt = Instant.now();

    public Long getId() { return id; }
    public BdayGuest getGuest() { return guest; }
    public void setGuest(BdayGuest guest) { this.guest = guest; }
    public int getBonusPoints() { return bonusPoints; }
    public void setBonusPoints(int bonusPoints) { this.bonusPoints = bonusPoints; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public Instant getSpunAt() { return spunAt; }
}
