package com.quizapp.security;

import com.quizapp.model.AppUser;
import com.quizapp.repository.AppUserRepository;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lets a player's sign-ins be withdrawn. Every USER token carries the account's token version ("tv"); changing or
 * resetting the password bumps it, so every token issued before stops working - a stolen one included - and a
 * deleted account has no version at all. A later account with the same email has a new id, so an old token (whose
 * "uid" is the deleted account's) can't reach it either.
 *
 * Looked up on every signed-in request, so kept in memory for a short while: a bump is seen everywhere within
 * CACHE_FOR (and straight away on this instance - bump() drops its own entry). Single-instance app; see
 * LoginRateLimiter's note on that.
 */
@Component
public class TokenVersions {

    private static final Duration CACHE_FOR = Duration.ofSeconds(30);

    private record Cached(Integer version, Instant loadedAt) {}

    private final AppUserRepository appUserRepository;
    private final Map<Long, Cached> cache = new ConcurrentHashMap<>();

    public TokenVersions(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    /** The account's current version, or null if there's no such account (deleted). */
    public Integer current(Long userId) {
        if (userId == null) return null;
        Cached hit = cache.get(userId);
        if (hit != null && hit.loadedAt().plus(CACHE_FOR).isAfter(Instant.now())) return hit.version();
        Integer version = appUserRepository.findById(userId).map(AppUser::getTokenVersion).orElse(null);
        if (cache.size() > 50_000) cache.clear(); // just a cache - never let it grow without bound
        cache.put(userId, new Cached(version, Instant.now()));
        return version;
    }

    /** Withdraws every sign-in issued so far for this account. The caller saves the user. */
    public void bump(AppUser user) {
        user.setTokenVersion(user.getTokenVersion() + 1);
        forget(user.getId());
    }

    public void forget(Long userId) {
        if (userId != null) cache.remove(userId);
    }
}
