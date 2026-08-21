package com.quizapp.service;

import com.quizapp.exception.GameAccessDeniedException;
import com.quizapp.model.AppUser;
import com.quizapp.repository.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class PlayAccessServiceTest {

    @Autowired
    private PlayAccessService playAccessService;
    @Autowired
    private AppUserRepository appUserRepository;

    private Authentication userAuth(String email) {
        return new UsernamePasswordAuthenticationToken(email, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    private Authentication adminAuth(String email) {
        return new UsernamePasswordAuthenticationToken(email, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    private AppUser newUser(boolean canPlayTension) {
        AppUser user = new AppUser();
        user.setEmail("player-" + System.nanoTime() + "@example.com");
        user.setName("Test Player");
        user.setCanPlayTension(canPlayTension);
        return appUserRepository.save(user);
    }

    @Test
    void allowsAUserWithTheFlagOn() {
        AppUser user = newUser(true);
        playAccessService.requireTensionAccess(userAuth(user.getEmail())); // does not throw
    }

    @Test
    void deniesAUserWithTheFlagOff() {
        AppUser user = newUser(false);
        assertThatThrownBy(() -> playAccessService.requireTensionAccess(userAuth(user.getEmail())))
                .isInstanceOf(GameAccessDeniedException.class)
                .hasMessageContaining("Tension");
    }

    @Test
    void newAccountsDefaultToAllowed() {
        // Mirrors what a brand new AppUser row looks like straight off signup -
        // never explicitly set, just the entity's own default.
        AppUser user = appUserRepository.save(freshAppUser());
        assertThat(user.isCanPlayTension()).isTrue();
        assertThat(user.isCanPlayGridBattle()).isTrue();
        assertThat(user.isCanPlayFiveOhOne()).isTrue();
        assertThat(user.isCanPlayImposter()).isTrue();
        assertThat(user.isCanPlayStartingXiBattle()).isTrue();
        assertThat(user.isCanPlayBullseye()).isTrue();
        playAccessService.requireGridBattleAccess(userAuth(user.getEmail())); // does not throw
    }

    private AppUser freshAppUser() {
        AppUser user = new AppUser();
        user.setEmail("fresh-" + System.nanoTime() + "@example.com");
        user.setName("Fresh Signup");
        return user;
    }

    @Test
    void adminAlwaysPassesRegardlessOfFlag() {
        AppUser user = newUser(false);
        // An admin JWT's subject wouldn't normally match an AppUser row at all,
        // but even pointed at one with the flag off, ROLE_ADMIN short-circuits.
        playAccessService.requireTensionAccess(adminAuth(user.getEmail())); // does not throw
    }

    @Test
    void adminWithNoMatchingAppUserRowStillPasses() {
        playAccessService.requireTensionAccess(adminAuth("admin-" + System.nanoTime() + "@example.com"));
    }

    @Test
    void deniesRatherThanErrorsWhenNoAppUserRowExists() {
        assertThatThrownBy(() -> playAccessService.requireTensionAccess(userAuth("nobody-" + System.nanoTime() + "@example.com")))
                .isInstanceOf(GameAccessDeniedException.class);
    }
}
