package com.quizapp.security;

import com.quizapp.dto.AuthResponse;
import com.quizapp.exception.TooManyAttemptsException;
import com.quizapp.model.AppUser;
import com.quizapp.repository.AppUserRepository;
import com.quizapp.service.AccountService;
import com.quizapp.service.AuthService;
import com.quizapp.service.MailService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// The security scan's fixes: a client can't pick its own IP for rate limiting, password guessing is capped per
// account whatever IP it claims, and a sign-in stops working once the password changes or the account is deleted.
@SpringBootTest
class SecurityHardeningTest {

    @Autowired private AuthService authService;
    @Autowired private AccountService accountService;
    @Autowired private JwtService jwtService;
    @Autowired private AppUserRepository appUserRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @MockBean private MailService mailService;

    private final List<AppUser> created = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        created.forEach(u -> appUserRepository.findById(u.getId()).ifPresent(appUserRepository::delete));
        created.clear();
    }

    private AppUser player(String password) {
        AppUser u = new AppUser();
        u.setEmail("hard-" + System.nanoTime() + "@example.com");
        u.setName("Hard " + System.nanoTime());
        u.setPasswordHash(passwordEncoder.encode(password));
        u.setEmailVerified(true);
        u = appUserRepository.save(u);
        created.add(u);
        return u;
    }

    private boolean accepted(String token) {
        return jwtService.isStillValid(jwtService.parseClaims(token));
    }

    @Test
    void theClientCantChooseItsOwnIp() {
        MockHttpServletRequest spoofed = new MockHttpServletRequest();
        spoofed.addHeader("X-Forwarded-For", "6.6.6.6, 203.0.113.9");
        assertThat(ClientIp.of(spoofed)).as("the entry Render appended, not the client's").isEqualTo("203.0.113.9");

        MockHttpServletRequest viaCloudflare = new MockHttpServletRequest();
        viaCloudflare.addHeader("True-Client-IP", "198.51.100.7");
        viaCloudflare.addHeader("X-Forwarded-For", "6.6.6.6, 203.0.113.9");
        assertThat(ClientIp.of(viaCloudflare)).isEqualTo("198.51.100.7");

        MockHttpServletRequest local = new MockHttpServletRequest();
        local.setRemoteAddr("127.0.0.1");
        assertThat(ClientIp.of(local)).isEqualTo("127.0.0.1");
    }

    @Test
    void passwordGuessingIsCappedPerAccountWhateverIpItClaims() {
        AppUser u = player("right-password-1");
        for (int i = 0; i < LoginRateLimiter.ACCOUNT_MAX_ATTEMPTS; i++) {
            String freshIp = "10.0." + (i / 200) + "." + (i % 200); // a new "IP" every time
            assertThatThrownBy(() -> authService.loginWithPassword(u.getEmail(), "wrong", freshIp))
                    .isInstanceOf(IllegalArgumentException.class);
        }
        assertThatThrownBy(() -> authService.loginWithPassword(u.getEmail(), "right-password-1", "10.9.9.9"))
                .as("even the right password, from yet another IP, waits out the window")
                .isInstanceOf(TooManyAttemptsException.class);
    }

    @Test
    void changingThePasswordWithdrawsOldSignInsButKeepsTheNewOne() {
        AppUser u = player("first-password-1");
        String oldToken = authService.loginWithPassword(u.getEmail(), "first-password-1", "10.1.1.1").getToken();
        assertThat(accepted(oldToken)).isTrue();

        AuthResponse fresh = accountService.changePassword(u.getEmail(), "first-password-1", "second-password-2");

        assertThat(accepted(oldToken)).as("the old sign-in (another device, or a stolen one)").isFalse();
        assertThat(accepted(fresh.getToken())).as("the one handed back to this device").isTrue();
        assertThatThrownBy(() -> jwtService.refresh(oldToken)).as("and it can't be renewed either")
                .isInstanceOf(io.jsonwebtoken.JwtException.class);
    }

    @Test
    void aDeletedAccountsSignInsStopWorkingEvenIfTheEmailIsUsedAgain() {
        AppUser u = player("some-password-1");
        String token = authService.loginWithPassword(u.getEmail(), "some-password-1", "10.2.2.2").getToken();

        accountService.deleteAccount(u.getEmail(), "some-password-1");
        assertThat(accepted(token)).isFalse();

        AppUser again = new AppUser(); // someone signs up with the same address later
        again.setEmail(u.getEmail());
        again.setName("Again " + System.nanoTime());
        again = appUserRepository.save(again);
        created.add(again);
        assertThat(accepted(token)).as("the old token names the deleted account's id, not the new one").isFalse();
    }

    @Test
    void tokensFromBeforeVersionsExistedStillWork() {
        AppUser u = player("legacy-password-1");
        // what a token issued before this change looks like: no "tv" claim at all
        String legacy = io.jsonwebtoken.Jwts.builder()
                .subject(u.getEmail()).claim("role", "USER").claim("uid", u.getId()).claim("name", u.getName())
                .issuedAt(new java.util.Date()).expiration(new java.util.Date(System.currentTimeMillis() + 60_000))
                .signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor(
                        "CHANGE-THIS-TO-A-LONG-RANDOM-SECRET-BEFORE-DEPLOYING-1234567890".getBytes()))
                .compact();
        assertThat(accepted(legacy)).isTrue();
    }
}
