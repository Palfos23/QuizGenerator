package com.quizapp.security;

import com.quizapp.exception.TooManyAttemptsException;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Basic fixed-window rate limiter for login-type attempts, keyed by client IP and/or account (see ClientIp).
 *
 * NOTE: this is in-memory and per-instance. It works fine for a single Render
 * web service instance (this app's whole deployment story), but would NOT
 * coordinate correctly across multiple instances of the app running behind a
 * load balancer - that would need a shared store (e.g. Redis) instead. Fine
 * for this project's scale; worth revisiting if that ever changes.
 */
@Component
public class LoginRateLimiter {

    private static final int MAX_ATTEMPTS = 5;
    /** For keys naming an ACCOUNT (not an IP): a little looser, since a stranger's failures count towards it too. */
    public static final int ACCOUNT_MAX_ATTEMPTS = 20;
    private static final Duration WINDOW = Duration.ofMinutes(15);
    // Every this many recorded attempts, entries whose window is over are swept out. Before, an entry was only
    // dropped when the same key came back - so a stream of never-repeated keys grew the map without end.
    private static final int SWEEP_EVERY = 500;
    private final java.util.concurrent.atomic.AtomicInteger sinceSweep = new java.util.concurrent.atomic.AtomicInteger();

    private final Map<String, Attempt> attemptsByKey = new ConcurrentHashMap<>();

    private static final class Attempt {
        int count;
        Instant windowStart;
    }

    /** Throws if this key has already failed too many times within the current window. */
    public void checkAllowed(String key) {
        checkAllowed(key, MAX_ATTEMPTS);
    }

    /** The same, with its own ceiling - see ACCOUNT_MAX_ATTEMPTS. */
    public void checkAllowed(String key, int maxAttempts) {
        Attempt attempt = attemptsByKey.get(key);
        if (attempt == null) {
            return;
        }
        if (windowExpired(attempt)) {
            attemptsByKey.remove(key);
            return;
        }
        if (attempt.count >= maxAttempts) {
            long secondsLeft = Duration.between(Instant.now(), attempt.windowStart.plus(WINDOW)).getSeconds();
            long minutesLeft = Math.max(1, (secondsLeft + 59) / 60);
            throw new TooManyAttemptsException(
                    "For mange mislykkede innloggingsforsøk. Prøv igjen om " + minutesLeft + " minutt(er).");
        }
    }

    public void recordFailure(String key) {
        if (sinceSweep.incrementAndGet() >= SWEEP_EVERY) {
            sinceSweep.set(0);
            attemptsByKey.entrySet().removeIf(e -> windowExpired(e.getValue()));
        }
        attemptsByKey.compute(key, (k, existing) -> {
            if (existing == null || windowExpired(existing)) {
                existing = new Attempt();
                existing.windowStart = Instant.now();
                existing.count = 0;
            }
            existing.count++;
            return existing;
        });
    }

    public void recordSuccess(String key) {
        attemptsByKey.remove(key);
    }

    private boolean windowExpired(Attempt attempt) {
        return Instant.now().isAfter(attempt.windowStart.plus(WINDOW));
    }
}
