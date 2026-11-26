package com.quizapp.service;

import com.quizapp.config.RequestLang;
import com.quizapp.repository.AppUserRepository;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Display names are unique, ignoring case ("Ola" and "ola" are the same name), so a name on a leaderboard
 * or in a league always means one player. Checked whenever a name is chosen: signing up with a password and
 * changing it on the account page. A Google sign-up takes its name from the Google profile without the player
 * typing anything, so instead of refusing it gets a free variant ("Ola Nordmann 2") they can change later.
 *
 * Enforced here rather than with a unique index: production already holds duplicates from before this rule
 * (they keep their names), and a unique constraint on that column couldn't be added under ddl-auto=update.
 */
@Service
public class DisplayNameService {

    private final AppUserRepository appUserRepository;

    public DisplayNameService(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    /** Whether someone other than {@code exceptUserId} (null = anyone) already uses this name. */
    public boolean isTaken(String name, Long exceptUserId) {
        return exceptUserId == null
                ? appUserRepository.existsByNameIgnoreCase(name)
                : appUserRepository.existsByNameIgnoreCaseAndIdNot(name, exceptUserId);
    }

    public void requireFree(String name, Long exceptUserId) {
        if (isTaken(name, exceptUserId)) {
            throw new IllegalArgumentException(RequestLang.t(
                    "Navnet er allerede i bruk av en annen spiller. Velg et annet.",
                    "That name is already used by another player. Please choose another."));
        }
    }

    /** The name itself if it's free, otherwise "name 2", "name 3", ... - always within the length limit. */
    public String freeVariant(String raw) {
        String base = raw == null ? "" : raw.trim().replaceAll("\\s+", " ");
        if (base.length() < AccountService.MIN_NAME_LENGTH) base = "Spiller";
        if (base.length() > AccountService.MAX_NAME_LENGTH) base = base.substring(0, AccountService.MAX_NAME_LENGTH).trim();
        if (!isTaken(base, null)) return base;
        for (int n = 2; n < 100; n++) {
            String candidate = withSuffix(base, " " + n);
            if (!isTaken(candidate, null)) return candidate;
        }
        while (true) {
            String candidate = withSuffix(base, " " + ThreadLocalRandom.current().nextInt(100, 100_000));
            if (!isTaken(candidate, null)) return candidate;
        }
    }

    private static String withSuffix(String base, String suffix) {
        int room = AccountService.MAX_NAME_LENGTH - suffix.length();
        return (base.length() > room ? base.substring(0, room).trim() : base) + suffix;
    }
}
