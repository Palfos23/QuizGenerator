package com.quizapp.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;

/**
 * What day the advent calendar thinks it is. Normally that is today's date in Norway - but the game master can set a
 * pretend date to try the calendar out before December (see AdventAdminService#setTestDate). A pretend date switches
 * itself off after a few hours, and a restart of the server also clears it, so it can't be left on by mistake.
 */
@Component
public class AdventClock {

    static final Duration SIMULATION_LENGTH = Duration.ofHours(3);

    private final Clock clock;
    private LocalDate simulatedDate;
    private Instant simulatedUntil;

    @Autowired
    public AdventClock() {
        this(Clock.systemUTC());
    }

    AdventClock(Clock clock) {
        this.clock = clock;
    }

    /** The real date in Norway, whatever is being simulated. */
    public synchronized LocalDate realToday() {
        return LocalDate.now(clock.withZone(AdventService.OSLO));
    }

    /** The date the calendar runs on: the pretend one while it lasts, otherwise the real one. */
    public synchronized LocalDate today() {
        expireIfDue();
        return simulatedDate != null ? simulatedDate : realToday();
    }

    public synchronized LocalDate simulatedDate() {
        expireIfDue();
        return simulatedDate;
    }

    public synchronized Instant simulatedUntil() {
        expireIfDue();
        return simulatedUntil;
    }

    public synchronized void simulate(LocalDate date) {
        this.simulatedDate = date;
        this.simulatedUntil = clock.instant().plus(SIMULATION_LENGTH);
    }

    public synchronized void clear() {
        this.simulatedDate = null;
        this.simulatedUntil = null;
    }

    private void expireIfDue() {
        if (simulatedUntil != null && !clock.instant().isBefore(simulatedUntil)) clear();
    }
}
