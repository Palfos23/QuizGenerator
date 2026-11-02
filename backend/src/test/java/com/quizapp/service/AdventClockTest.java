package com.quizapp.service;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

// The advent calendar's clock: today in Norway, or a pretend date that switches itself off.
class AdventClockTest {

    /** A clock the test can move. */
    private static final class MovableClock extends Clock {
        Instant now;

        MovableClock(Instant now) { this.now = now; }

        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return Clock.fixed(now, zone); }
        @Override public Instant instant() { return now; }
    }

    @Test
    void theRealDateIsTheDateInNorwayNotInUtc() {
        // 23:30 UTC on 30 November is already 1 December in Norway (UTC+1)
        AdventClock clock = new AdventClock(new MovableClock(Instant.parse("2031-11-30T23:30:00Z")));
        assertThat(clock.realToday()).isEqualTo(LocalDate.of(2031, 12, 1));
        assertThat(clock.today()).isEqualTo(LocalDate.of(2031, 12, 1));
    }

    @Test
    void aPretendDateReplacesTheRealOneForThreeHoursAndThenSwitchesItselfOff() {
        MovableClock real = new MovableClock(Instant.parse("2031-11-10T12:00:00Z"));
        AdventClock clock = new AdventClock(real);

        clock.simulate(LocalDate.of(2031, 12, 5));
        assertThat(clock.today()).isEqualTo(LocalDate.of(2031, 12, 5));
        assertThat(clock.realToday()).isEqualTo(LocalDate.of(2031, 11, 10));
        assertThat(clock.simulatedDate()).isEqualTo(LocalDate.of(2031, 12, 5));

        real.now = Instant.parse("2031-11-10T14:59:00Z");
        assertThat(clock.today()).isEqualTo(LocalDate.of(2031, 12, 5));
        real.now = Instant.parse("2031-11-10T15:00:00Z");
        assertThat(clock.today()).isEqualTo(LocalDate.of(2031, 11, 10));
        assertThat(clock.simulatedDate()).isNull();
    }

    @Test
    void thePretendDateCanBeSwitchedOffAtOnce() {
        AdventClock clock = new AdventClock(new MovableClock(Instant.parse("2031-11-10T12:00:00Z")));
        clock.simulate(LocalDate.of(2031, 12, 5));
        clock.clear();
        assertThat(clock.today()).isEqualTo(LocalDate.of(2031, 11, 10));
    }
}
