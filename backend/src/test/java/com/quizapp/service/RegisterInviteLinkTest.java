package com.quizapp.service;

import com.quizapp.repository.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

// Signing up from a league invite: the confirmation email's link carries the invite code, so confirming the
// address (on any device) brings the new player back to the invite instead of the dashboard.
@SpringBootTest
class RegisterInviteLinkTest {

    @Autowired private AuthService authService;
    @Autowired private AppUserRepository appUserRepository;
    @MockBean private MailService mailService;

    private String register(String leagueCode) {
        String email = "invite-" + System.nanoTime() + "@example.com";
        try {
            authService.registerWithPassword(email, "long-enough-password", "Invitee " + System.nanoTime(), leagueCode,
                    "test-" + System.nanoTime());
            ArgumentCaptor<String> link = ArgumentCaptor.forClass(String.class);
            verify(mailService).sendVerificationEmail(eq(email), link.capture());
            return link.getValue();
        } finally {
            appUserRepository.findByEmail(email).ifPresent(appUserRepository::delete);
        }
    }

    @Test
    void theInviteCodeRidesAlongInTheVerificationLink() {
        assertThat(register("k7m2qx")).contains("/verify-email?token=").endsWith("&league=K7M2QX");
    }

    @Test
    void noOrMalformedCodeLeavesThePlainLink() {
        assertThat(register(null)).doesNotContain("league=");
        assertThat(register("bad code&x=1")).doesNotContain("league=");
    }
}
