package com.quizapp.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
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
    private final long expirationMinutes;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-minutes:720}") long expirationMinutes
    ) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMinutes = expirationMinutes;
    }

    /** subject = a stable identifier (email for users, username for admins), role = USER or ADMIN */
    public String generateToken(String subject, String role, Long userId, String displayName) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(subject)
                .claim("role", role)
                .claim("uid", userId)
                .claim("name", displayName)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expirationMinutes, ChronoUnit.MINUTES)))
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

    /** token + the role/displayName it carries, so a caller building a response
     *  DTO (AuthController#refresh) doesn't need to re-parse the fresh token
     *  right back apart again just to read those two fields off it. */
    public record RefreshResult(String token, String role, String displayName) {}

    /**
     * Silently renews a still-valid token with a fresh full-length expiry, carrying
     * over the same subject/role/uid/name - the sliding-session half of fixing
     * "logged out too often": an actively-used tab keeps renewing itself well before
     * its token would actually expire (see AuthController#refresh, called from
     * App.vue's checkSessionTimers), so expirationMinutes effectively only matters
     * for a session that's genuinely gone unused. The separate, much shorter
     * inactivity timeout in App.vue still logs out a truly abandoned/shared-device
     * tab regardless of this.
     * parseClaims() throws (JwtException) for an already-expired or tampered token -
     * deliberately left uncaught here, since refreshing an expired token would
     * defeat the point of it expiring at all.
     */
    public RefreshResult refresh(String oldToken) {
        Claims claims = parseClaims(oldToken);
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
