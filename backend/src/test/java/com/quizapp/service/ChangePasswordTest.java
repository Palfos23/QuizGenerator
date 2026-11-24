package com.quizapp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.quizapp.exception.TooManyAttemptsException;
import com.quizapp.model.AppUser;
import com.quizapp.repository.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

// A signed-in user choosing a new password from their profile.
@SpringBootTest
class ChangePasswordTest {

    @Autowired private AccountService accountService;
    @Autowired private AppUserRepository appUserRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private AppUser userWithPassword(String password) {
        AppUser u = new AppUser();
        u.setEmail("pw-" + System.nanoTime() + "@example.com");
        u.setName("Password Person");
        u.setPasswordHash(passwordEncoder.encode(password));
        u.setResetTokenHash("some-pending-reset");
        return appUserRepository.save(u);
    }

    private AppUser reload(AppUser u) {
        return appUserRepository.findById(u.getId()).orElseThrow();
    }

    @Test
    void theNewPasswordReplacesTheOldOneAndPendingResetLinksStopWorking() {
        AppUser u = userWithPassword("old-password-1");

        accountService.changePassword(u.getEmail(), "old-password-1", "brand-new-pass");

        AppUser after = reload(u);
        assertThat(passwordEncoder.matches("brand-new-pass", after.getPasswordHash())).isTrue();
        assertThat(passwordEncoder.matches("old-password-1", after.getPasswordHash())).isFalse();
        assertThat(after.getResetTokenHash()).isNull();
    }

    @Test
    void aWrongCurrentPasswordChangesNothing() {
        AppUser u = userWithPassword("old-password-1");

        assertThatThrownBy(() -> accountService.changePassword(u.getEmail(), "not-my-password", "brand-new-pass"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Feil nåværende passord");
        assertThatThrownBy(() -> accountService.changePassword(u.getEmail(), null, "brand-new-pass"))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(passwordEncoder.matches("old-password-1", reload(u).getPasswordHash())).isTrue();
    }

    @Test
    void theNewPasswordMustBeLongEnoughDifferentAndUsable() {
        AppUser u = userWithPassword("old-password-1");

        assertThatThrownBy(() -> accountService.changePassword(u.getEmail(), "old-password-1", "short"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("minst 8");
        assertThatThrownBy(() -> accountService.changePassword(u.getEmail(), "old-password-1", "old-password-1"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("forskjellig");
        // bcrypt only reads the first 72 bytes - a longer password would silently be cut off, so it is refused
        assertThatThrownBy(() -> accountService.changePassword(u.getEmail(), "old-password-1", "x".repeat(80)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("72");

        assertThat(passwordEncoder.matches("old-password-1", reload(u).getPasswordHash())).isTrue();
    }

    @Test
    void aGoogleOnlyAccountHasNoPasswordToChange() {
        AppUser g = new AppUser();
        g.setEmail("google-" + System.nanoTime() + "@example.com");
        g.setName("Google Person");
        g = appUserRepository.save(g);
        String email = g.getEmail();

        assertThat(accountService.hasPassword(email)).isFalse();
        assertThat(accountService.hasPassword(userWithPassword("old-password-1").getEmail())).isTrue();
        assertThatThrownBy(() -> accountService.changePassword(email, "anything", "brand-new-pass"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Google");
    }

    @Test
    void repeatedWrongGuessesAreCutOff() {
        AppUser u = userWithPassword("old-password-1");
        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> accountService.changePassword(u.getEmail(), "guess", "brand-new-pass"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
        // even the right password is refused until the window passes
        assertThatThrownBy(() -> accountService.changePassword(u.getEmail(), "old-password-1", "brand-new-pass"))
                .isInstanceOf(TooManyAttemptsException.class);
    }
}
