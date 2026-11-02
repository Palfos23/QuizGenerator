package com.quizapp.security;

import com.quizapp.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

// The advent calendar is for signed-in accounts only, and its game master side is for admins only.
@SpringBootTest
@AutoConfigureMockMvc
class AdventAccessTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtService jwtService;
    @Autowired private AuthService authService;

    // each request comes from its own made-up client address, so deliberate wrong guesses never lock out another test
    private int next = 0;

    private String ip() {
        return "10.78.0." + (next++ % 250);
    }

    private int status(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request, String token) throws Exception {
        if (token != null) request.header("Authorization", "Bearer " + token);
        return mockMvc.perform(request).andReturn().getResponse().getStatus();
    }

    @Test
    void youHaveToBeSignedInToSeeTheCalendar() throws Exception {
        assertThat(status(get("/api/advent"), null)).isIn(401, 403);
        assertThat(status(get("/api/advent/days/1"), null)).isIn(401, 403);
        assertThat(status(get("/api/advent/leaderboard"), null)).isIn(401, 403);
        assertThat(status(put("/api/advent/profile").contentType("application/json").content("{\"showName\":false}"), null)).isIn(401, 403);
    }

    @Test
    void aGuestWithoutAnAccountCannotUseIt() throws Exception {
        String guest = authService.loginAsGuest("Guest", "advent-guest-" + System.nanoTime()).getToken();
        assertThat(status(get("/api/advent"), guest)).isIn(401, 403);
    }

    @Test
    void aSignedInPlayerCanButTheGamemastersSideIsForAdminsOnly() throws Exception {
        String player = jwtService.generateToken("advent-access@example.com", "USER", 987654L, "Spiller");
        assertThat(status(get("/api/advent"), player)).isEqualTo(200);
        assertThat(status(get("/api/admin/advent"), player)).isIn(401, 403);
        assertThat(status(get("/api/admin/advent/days/1/review"), player)).isIn(401, 403);
        assertThat(status(get("/api/admin/advent"), null)).isIn(401, 403);

        String admin = jwtService.generateToken("admin", "ADMIN", 2L, "admin");
        assertThat(status(get("/api/admin/advent").header("X-Advent-Password", "jul123").header("X-Forwarded-For", ip()), admin)).isEqualTo(200);
    }

    @Test
    void theGamemastersSideAlsoNeedsTheCalendarPasswordOnTopOfTheAdminLogin() throws Exception {
        String admin = jwtService.generateToken("admin", "ADMIN", 2L, "admin");
        for (String path : new String[] {"/api/admin/advent", "/api/admin/advent/days/1", "/api/admin/advent/days/1/review"}) {
            // an admin without the password, or with a wrong one, is refused - with 403, never 401 (a 401 logs people out)
            assertThat(status(get(path).header("X-Forwarded-For", ip()), admin)).isEqualTo(403);
            assertThat(status(get(path).header("X-Advent-Password", "feil").header("X-Forwarded-For", ip()), admin)).isEqualTo(403);
            // the password alone is not enough: a signed-in player who knows it is still not an admin
            String player = jwtService.generateToken("advent-access@example.com", "USER", 987654L, "Spiller");
            assertThat(status(get(path).header("X-Advent-Password", "jul123").header("X-Forwarded-For", ip()), player)).isIn(401, 403);
            // and nobody who isn't signed in can even try guesses
            assertThat(status(get(path).header("X-Advent-Password", "jul123").header("X-Forwarded-For", ip()), null)).isIn(401, 403);
        }
        assertThat(status(get("/api/admin/advent/days/1").header("X-Advent-Password", "jul123").header("X-Forwarded-For", ip()), admin)).isEqualTo(200);
    }

    @Test
    void anAdminGuessingThePasswordGetsLockedOutForAWhile() throws Exception {
        String admin = jwtService.generateToken("admin", "ADMIN", 2L, "admin");
        String sameClient = "10.77.0." + (next++ % 250);
        int last = 0;
        for (int i = 0; i < 12 && last != 429; i++) {
            last = status(get("/api/admin/advent").header("X-Advent-Password", "gjetning" + i).header("X-Forwarded-For", sameClient), admin);
        }
        assertThat(last).isEqualTo(429);
        // even the right password is refused during the lockout
        assertThat(status(get("/api/admin/advent").header("X-Advent-Password", "jul123").header("X-Forwarded-For", sameClient), admin)).isEqualTo(429);
    }
}
