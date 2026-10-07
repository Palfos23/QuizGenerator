package com.quizapp.security;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private final JwtService jwtService = new JwtService("test-secret-test-secret-test-secret-test-secret", 720, 43200);

    private long lifetimeMinutes(String token) {
        Claims claims = jwtService.parseClaims(token);
        return Duration.between(claims.getIssuedAt().toInstant(), claims.getExpiration().toInstant()).toMinutes();
    }

    @Test
    void playersGetALongSessionWhileAdminsAndGuestsKeepTheShortOne() {
        assertThat(lifetimeMinutes(jwtService.generateToken("a@example.com", "USER", 1L, "A"))).isEqualTo(43200);
        assertThat(lifetimeMinutes(jwtService.generateToken("admin", "ADMIN", 2L, "admin"))).isEqualTo(720);
        assertThat(lifetimeMinutes(jwtService.generateToken("guest-1", "GUEST", null, "G"))).isEqualTo(720);
    }

    @Test
    void refreshKeepsTheRolesLifetime() {
        String userToken = jwtService.generateToken("a@example.com", "USER", 1L, "A");
        assertThat(lifetimeMinutes(jwtService.refresh(userToken).token())).isEqualTo(43200);

        String adminToken = jwtService.generateToken("admin", "ADMIN", 2L, "admin");
        assertThat(lifetimeMinutes(jwtService.refresh(adminToken).token())).isEqualTo(720);
    }
}
