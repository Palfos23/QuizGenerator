package com.quizapp.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.JwtException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey key;
    // Admins and guests: short-lived (24h by default) - an admin session can change anything, and a
    // guest token only has to outlast one game night.
    private final long expirationMinutes;
    // Regular players: a long, sliding session (90 days by default). The frontend renews the token
    // whenever the app is opened, so in practice a player who comes back at least once a month never
    // sees a sign-in screen; the old 12h cap used to log out anyone who skipped a day (the daily quiz
    // habit - play in the evening, come back the next morning).
    private final long userExpirationMinutes;
    // null only in unit tests that build this by hand - then no token is ever withdrawn
    private final TokenVersions tokenVersions;

    @Autowired
    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-minutes:1440}") long expirationMinutes,
            @Value("${app.jwt.user-expiration-minutes:129600}") long userExpirationMinutes,
            TokenVersions tokenVersions
    ) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMinutes = expirationMinutes;
        this.userExpirationMinutes = userExpirationMinutes;
        this.tokenVersions = tokenVersions;
    }

    public JwtService(String secret, long expirationMinutes, long userExpirationMinutes) {
        this(secret, expirationMinutes, userExpirationMinutes, null);
    }

    /** subject = a stable identifier (email for users, username for admins), role = USER or ADMIN */
    public String generateToken(String subject, String role, Long userId, String displayName) {
        Instant now = Instant.now();
        long lifetimeMinutes = "USER".equals(role) ? userExpirationMinutes : expirationMinutes;
        Integer version = "USER".equals(role) && tokenVersions != null ? tokenVersions.current(userId) : null;
        return Jwts.builder()
                .subject(subject)
                .claim("role", role)
                .claim("uid", userId)
                .claim("tv", version)
                .claim("name", displayName)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(lifetimeMinutes, ChronoUnit.MINUTES)))
                .signWith(key)
                .compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Whether a (correctly signed, unexpired) token still counts: a player's token is withdrawn once their password
     * changes or the account is gone (see TokenVersions). A token from before versions existed has none - it counts
     * as version 0, which every account started at, so nobody was signed out by this arriving.
     */
    public boolean isStillValid(Claims claims) {
        if (tokenVersions == null || !"USER".equals(claims.get("role", String.class))) return true;
        Object uidClaim = claims.get("uid");
        if (!(uidClaim instanceof Number uid)) return false;
        Integer current = tokenVersions.current(uid.longValue());
        if (current == null) return false; // the account is gone
        Object tv = claims.get("tv");
        int tokenVersion = tv instanceof Number n ? n.intValue() : 0;
        return tokenVersion == current;
    }

    /** token + the role/displayName it carries, so a caller building a response
     *  DTO (AuthController#refresh) doesn't need to re-parse the fresh token
     *  right back apart again just to read those two fields off it. */
    public record RefreshResult(String token, String role, String displayName) {}

    /**
     * Silently renews a still-valid token with a fresh full-length expiry, carrying
     * over the same subject/role/uid/name - the sliding-session half of fixing
     * "logged out too often": an actively-used tab keeps renewing itself well before
     * its token would actually expire (see AuthController#refresh, called from
     * App.vue's checkSessionTimers), so the expiry only matters
     * for a session that's genuinely gone unused. The separate, much shorter
     * inactivity timeout in App.vue still logs out a truly abandoned/shared-device
     * tab regardless of this.
     * parseClaims() throws (JwtException) for an already-expired or tampered token -
     * deliberately left uncaught here, since refreshing an expired token would
     * defeat the point of it expiring at all.
     */
    public RefreshResult refresh(String oldToken) {
        Claims claims = parseClaims(oldToken);
        // A withdrawn sign-in can't be renewed into a fresh one.
        if (!isStillValid(claims)) throw new JwtException("This sign-in has been withdrawn.");
        String role = claims.get("role", String.class);
        String name = claims.get("name", String.class);
        // Read as Number rather than claims.get("uid", Long.class) - JJWT/Jackson
        // deserializes a JSON integer as Integer, not Long, unless the value is
        // actually outside Integer's range, so a direct Long.class cast can throw
        // ClassCastException even though the claim was written as a Long.
        Object uidClaim = claims.get("uid");
        Long uid = uidClaim == null ? null : ((Number) uidClaim).longValue();
        String fresh = generateToken(claims.getSubject(), role, uid, name);
        return new RefreshResult(fresh, role, name);
    }
}
