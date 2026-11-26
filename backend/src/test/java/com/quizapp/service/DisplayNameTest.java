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
    @Autowired private DisplayNameService displayNameService;

    private AppUser newUser() {
        return newUser("Original Name");
    }

    private AppUser newUser(String name) {
        AppUser u = new AppUser();
        u.setEmail("rename-" + System.nanoTime() + "@example.com");
        u.setName(name);
        return appUserRepository.save(u);
    }

    @Test
    void aNameAnotherPlayerUsesCantBeTakenWhateverTheCapitalisation() {
        String taken = "Unique Ola " + System.nanoTime() % 100000;
        newUser(taken);
        AppUser other = newUser("Someone " + System.nanoTime() % 100000);

        assertThatThrownBy(() -> accountService.updateDisplayName(other.getEmail(), taken.toLowerCase()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("allerede i bruk");
        assertThatThrownBy(() -> accountService.updateDisplayName(other.getEmail(), "  " + taken.toUpperCase() + " "))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(appUserRepository.findById(other.getId()).orElseThrow().getName()).startsWith("Someone");
    }

    @Test
    void keepingYourOwnNameOrChangingItsCapitalisationIsFine() {
        String mine = "Kari Self " + System.nanoTime() % 100000;
        AppUser me = newUser(mine);
        accountService.updateDisplayName(me.getEmail(), mine);
        accountService.updateDisplayName(me.getEmail(), mine.toLowerCase());
        assertThat(appUserRepository.findById(me.getId()).orElseThrow().getName()).isEqualTo(mine.toLowerCase());
    }

    @Test
    void aClashingGoogleNameGetsAFreeVariant() {
        String base = "Google Ola " + System.nanoTime() % 100000;
        assertThat(displayNameService.freeVariant(base)).as("free as it is").isEqualTo(base);
        newUser(base);
        assertThat(displayNameService.freeVariant(base.toLowerCase())).isEqualToIgnoringCase(base + " 2");
        newUser(base + " 2");
        assertThat(displayNameService.freeVariant(base)).isEqualTo(base + " 3");

        String longName = "L".repeat(38) + System.nanoTime() % 10; // 39 chars
        newUser(longName);
        String variant = displayNameService.freeVariant(longName);
        assertThat(variant).hasSizeLessThanOrEqualTo(AccountService.MAX_NAME_LENGTH).endsWith(" 2");
        assertThat(displayNameService.freeVariant(null)).isNotBlank();
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
            AuthResponse response = accountService.updateDisplayName(user.getEmail(), "  Erik  H" + user.getId() + " ");

            assertThat(response.getDisplayName()).isEqualTo("Erik H" + user.getId());
            assertThat(appUserRepository.findById(user.getId()).orElseThrow().getName()).isEqualTo("Erik H" + user.getId());
            assertThat(resultRepository.findByUserIdAndQuizDate(user.getId(), r.getQuizDate()).orElseThrow().getPlayerName()).isEqualTo("Erik H" + user.getId());
            assertThat(jwtService.parseClaims(response.getToken()).get("name", String.class)).isEqualTo("Erik H" + user.getId());
            assertThat(jwtService.parseClaims(response.getToken()).getSubject()).isEqualTo(user.getEmail());
        } finally {
            resultRepository.deleteByUserId(user.getId());
        }
    }
}
