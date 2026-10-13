package com.quizapp.service;

import com.quizapp.dto.AuthResponse;
import com.quizapp.model.AppUser;
import com.quizapp.model.DailyQuizResult;
import com.quizapp.repository.AppUserRepository;
import com.quizapp.repository.DailyQuizResultRepository;
import com.quizapp.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class DisplayNameTest {

    @Autowired private AccountService accountService;
    @Autowired private AppUserRepository appUserRepository;
    @Autowired private DailyQuizResultRepository resultRepository;
    @Autowired private JwtService jwtService;

    private AppUser newUser() {
        AppUser u = new AppUser();
        u.setEmail("rename-" + System.nanoTime() + "@example.com");
        u.setName("Original Name");
        return appUserRepository.save(u);
    }

    @Test
    void namesAreTidiedAndBadOnesRejected() {
        assertThat(AccountService.cleanDisplayName("  Erik   Hansen ")).isEqualTo("Erik Hansen");
        assertThatThrownBy(() -> AccountService.cleanDisplayName("  ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> AccountService.cleanDisplayName("E")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> AccountService.cleanDisplayName(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> AccountService.cleanDisplayName("x".repeat(41))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> AccountService.cleanDisplayName("<script>")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void renamingUpdatesTheUserTheRetainedResultsAndIssuesATokenWithTheNewName() {
        AppUser user = newUser();
        DailyQuizResult r = new DailyQuizResult();
        r.setUserId(user.getId());
        r.setQuizDate(LocalDate.now().plusYears(70));
        r.setPlayerName("Original Name");
        r.setScore(5);
        r.setMaxScore(16);
        resultRepository.save(r);
        try {
            AuthResponse response = accountService.updateDisplayName(user.getEmail(), "  Erik  H ");

            assertThat(response.getDisplayName()).isEqualTo("Erik H");
            assertThat(appUserRepository.findById(user.getId()).orElseThrow().getName()).isEqualTo("Erik H");
            assertThat(resultRepository.findByUserIdAndQuizDate(user.getId(), r.getQuizDate()).orElseThrow().getPlayerName()).isEqualTo("Erik H");
            assertThat(jwtService.parseClaims(response.getToken()).get("name", String.class)).isEqualTo("Erik H");
            assertThat(jwtService.parseClaims(response.getToken()).getSubject()).isEqualTo(user.getEmail());
        } finally {
            resultRepository.deleteByUserId(user.getId());
        }
    }
}
