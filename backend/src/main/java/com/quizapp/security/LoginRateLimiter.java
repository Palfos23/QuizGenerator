package com.quizapp.security;

import com.quizapp.exception.TooManyAttemptsException;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Basic fixed-window rate limiter for admin login attempts, keyed by client IP.
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
    private static final Duration WINDOW = Duration.ofMinutes(15);

    private final Map<String, Attempt> attemptsByKey = new ConcurrentHashMap<>();

    private static final class Attempt {
        int count;
        Instant windowStart;
    }

    /** Throws if this key has already failed too many times within the current window. */
    public void checkAllowed(String key) {
        Attempt attempt = attemptsByKey.get(key);
        if (attempt == null) {
            return;
        }
        if (windowExpired(attempt)) {
            attemptsByKey.remove(key);
            return;
        }
        if (attempt.count >= MAX_ATTEMPTS) {
            long secondsLeft = Duration.between(Instant.now(), attempt.windowStart.plus(WINDOW)).getSeconds();
            long minutesLeft = Math.max(1, (secondsLeft + 59) / 60);
            throw new TooManyAttemptsException(
                    "Too many failed login attempts. Try again in " + minutesLeft + " minute(s).");
        }
    }

    public void recordFailure(String key) {
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
