package com.quizapp.security;

import com.quizapp.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

// A GUEST (joined a room by code/QR, no account) is deliberately locked out of nearly the whole
// API - SecurityConfig only lets them reach /api/rooms/** and a short list of read-only lookups
// the games call mid-round. Every such lookup must be on that list AND skip the per-account
// play-access check; missing either one gives a silent 403 and a broken game for guests (this is
// exactly how Tension subject suggestions broke). This walks every request a guest's browser makes
// during an online game, using ids that don't exist: a guest who's allowed in gets a 404/400/200,
// while one who's wrongly refused gets 401/403 - which is the failure being guarded against.
@SpringBootTest
@AutoConfigureMockMvc
class GuestAccessTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private AuthService authService;

    private record Call(HttpMethod method, String path, String body) {
        static Call get(String path) { return new Call(HttpMethod.GET, path, null); }
        static Call post(String path, String body) { return new Call(HttpMethod.POST, path, body); }
    }

    private int statusOf(Call call, String token) throws Exception {
        var builder = request(call.method(), call.path());
        if (token != null) builder.header("Authorization", "Bearer " + token);
        if (call.body() != null) builder.contentType("application/json").content(call.body());
        MvcResult result = mockMvc.perform(builder).andReturn();
        return result.getResponse().getStatus();
    }

    private String guestToken() {
        return authService.loginAsGuest("Guest", "guest-access-" + System.nanoTime()).getToken();
    }

    private static final String ROOM = "/api/rooms/ZZZZZZ";

    @Test
    void everyRequestAGuestMakesDuringAnOnlineGameIsAllowedThrough() throws Exception {
        List<Call> needed = List.of(
                // room lifecycle
                Call.get(ROOM),
                Call.post(ROOM + "/join", "{\"displayName\":\"Guest\"}"),
                Call.post(ROOM + "/heartbeat", null),
                // per-game state polling
                Call.get(ROOM + "/tension/state"),
                Call.get(ROOM + "/501/state"),
                Call.get(ROOM + "/imposter/state"),
                Call.get(ROOM + "/imposter/reveal"),
                Call.get(ROOM + "/grid-battle/state"),
                Call.get(ROOM + "/lineup-battle/state"),
                Call.get(ROOM + "/bullseye/state"),
                Call.get(ROOM + "/flashback/state"),
                // answer-box lookups outside /api/rooms - the ones that need their own allow-list entry
                Call.get("/api/tension/categories/Anything/options"),
                Call.get("/api/tension/questions/subject-options?sport=Football"),
                Call.get("/api/501/categories/999999"),
                Call.get("/api/grids/999999/candidates?search=ab"),
                Call.get("/api/grids/999999/reveal-all"),
                Call.get("/api/lineups/999999/candidates?search=ab"),
                Call.get("/api/lineups/999999/reveal-all"),
                // problem reports from the browser
                Call.post("/api/diagnostics/client-event", "{\"area\":\"test\",\"kind\":\"TEST\"}")
        );

        String token = guestToken();
        List<String> wronglyRefused = new ArrayList<>();
        for (Call call : needed) {
            int status = statusOf(call, token);
            if (status == 401 || status == 403) {
                wronglyRefused.add(call.method() + " " + call.path() + " -> " + status);
            }
        }

        assertThat(wronglyRefused).as("endpoints a guest needs mid-game but is refused").isEmpty();
    }

    @Test
    void guestsAreStillKeptOutOfEverythingElse() throws Exception {
        List<Call> forbidden = List.of(
                Call.get("/api/quiz/saved"),
                Call.get("/api/daily-quiz/active"),
                Call.get("/api/grids/active"),
                Call.get("/api/lineups"),
                Call.get("/api/tension/questions/random"),
                Call.get("/api/tension/questions/round-choices"),
                Call.get("/api/501/categories"),
                Call.get("/api/bullseye/categories"),
                Call.get("/api/account/export"),
                Call.get("/api/admin/questions")
        );

        String token = guestToken();
        List<String> wronglyAllowed = new ArrayList<>();
        for (Call call : forbidden) {
            int status = statusOf(call, token);
            if (status != 403) {
                wronglyAllowed.add(call.method() + " " + call.path() + " -> " + status);
            }
        }

        assertThat(wronglyAllowed).as("endpoints a guest must NOT be able to reach").isEmpty();
    }
}
