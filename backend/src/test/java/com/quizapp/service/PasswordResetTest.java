package com.quizapp.service;

import com.quizapp.dto.AuthResponse;
import com.quizapp.model.AppUser;
import com.quizapp.repository.AppUserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

// "Glemt passord": a Google account is told by email to use Google, a reset logs the player in and confirms an
// account that was never confirmed, and a password bcrypt would cut short is refused.
@SpringBootTest
class PasswordResetTest {

    @Autowired private AuthService authService;
    @Autowired private AppUserRepository appUserRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @MockBean private MailService mailService;

    private final List<AppUser> created = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        appUserRepository.deleteAll(created);
        created.clear();
    }

    private AppUser user(boolean withPassword, boolean verified) {
        AppUser u = new AppUser();
        u.setEmail("reset-" + System.nanoTime() + "@example.com");
        u.setName("Reset " + System.nanoTime());
        if (withPassword) u.setPasswordHash(passwordEncoder.encode("old-password-123"));
        u.setEmailVerified(verified);
        u = appUserRepository.save(u);
        created.add(u);
        return u;
    }

    private String requestLinkToken(AppUser u) {
        authService.requestPasswordReset(u.getEmail(), "test-" + System.nanoTime());
        ArgumentCaptor<String> link = ArgumentCaptor.forClass(String.class);
        verify(mailService).sendPasswordResetEmail(eq(u.getEmail()), link.capture());
        return link.getValue().substring(link.getValue().indexOf("token=") + 6);
    }

    @Test
    void aGoogleAccountIsPointedToGoogleInsteadOfGettingNothing() {
        AppUser google = user(false, true);
        authService.requestPasswordReset(google.getEmail(), "test-" + System.nanoTime());
        verify(mailService).sendGoogleAccountHintEmail(eq(google.getEmail()), anyString());
        verify(mailService, never()).sendPasswordResetEmail(anyString(), anyString());
    }

    @Test
    void anUnknownAddressStillGetsNoEmail() {
        authService.requestPasswordReset("nobody-" + System.nanoTime() + "@example.com", "test-" + System.nanoTime());
        verify(mailService, never()).sendPasswordResetEmail(anyString(), anyString());
        verify(mailService, never()).sendGoogleAccountHintEmail(anyString(), anyString());
    }

    @Test
    void aResetLogsInAndConfirmsANeverConfirmedAccount() {
        AppUser u = user(true, false);
        AuthResponse response = authService.resetPassword(requestLinkToken(u), "brand-new-password");

        assertThat(response.getToken()).isNotBlank();
        AppUser after = appUserRepository.findById(u.getId()).orElseThrow();
        assertThat(after.isEmailVerified()).isTrue();
        assertThat(passwordEncoder.matches("brand-new-password", after.getPasswordHash())).isTrue();
        assertThat(after.getResetTokenHash()).as("the link is single-use").isNull();
        // and the new password now works for an ordinary sign-in
        assertThat(authService.loginWithPassword(u.getEmail(), "brand-new-password", "test-" + System.nanoTime()).getToken()).isNotBlank();
    }

    @Test
    void aPasswordLongerThanBcryptCanUseIsRefused() {
        AppUser u = user(true, true);
        String token = requestLinkToken(u);
        assertThatThrownBy(() -> authService.resetPassword(token, "x".repeat(73)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("72");
        assertThat(passwordEncoder.matches("old-password-123", appUserRepository.findById(u.getId()).orElseThrow().getPasswordHash()))
                .as("unchanged").isTrue();
    }
}
