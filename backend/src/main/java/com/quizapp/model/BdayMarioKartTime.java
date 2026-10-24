package com.quizapp.model;

import jakarta.persistence.*;

import java.time.Instant;

// A guest's best lap in the Mario Kart competition, in milliseconds. One per guest - the first time they enter wins until
// the host changes or removes it. The points it is worth come from the guest's rank among all times (BdayStandingsService).
@Entity
@Table(name = "bday_mario_kart_times")
public class BdayMarioKartTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "guest_id", nullable = false, unique = true)
    private BdayGuest guest;

    @Column(nullable = false)
    private int timeMs;

    @Column(nullable = false)
    private Instant enteredAt = Instant.now();

    public Long getId() { return id; }
    public BdayGuest getGuest() { return guest; }
    public void setGuest(BdayGuest guest) { this.guest = guest; }
    public int getTimeMs() { return timeMs; }
    public void setTimeMs(int timeMs) { this.timeMs = timeMs; this.enteredAt = Instant.now(); }
    public Instant getEnteredAt() { return enteredAt; }
}
