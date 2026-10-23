package com.quizapp.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// The birthday quiz has two sides: guests play on their own phones with no login and no PIN, and everything the
// host does (players, questions, score fixes) needs the real admin login - nobody else may even look.
@SpringBootTest
@AutoConfigureMockMvc
class BdayAccessTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtService jwtService;

    @Test
    void guestsReachTheQuizWithoutLoggingInOrAnyPin() throws Exception {
        mockMvc.perform(get("/api/bday/guests")).andExpect(status().isOk());
        mockMvc.perform(get("/api/bday/leaderboard")).andExpect(status().isOk());
    }

    @Test
    void theHostsSideIsClosedToAnonymousVisitorsAndOrdinaryUsers() throws Exception {
        for (String path : new String[] {"/api/admin/bday/guests", "/api/admin/bday/questions"}) {
            int anonymous = mockMvc.perform(get(path)).andReturn().getResponse().getStatus();
            assertThat(anonymous).isIn(401, 403);

            String userToken = jwtService.generateToken("someone@example.com", "USER", 5L, "Someone");
            mockMvc.perform(get(path).header("Authorization", "Bearer " + userToken)).andExpect(status().isForbidden());
        }
        // ...and a stranger can't add a player or a question either
        int addPlayer = mockMvc.perform(post("/api/admin/bday/guests").contentType("application/json").content("{\"name\":\"Hacker\"}"))
                .andReturn().getResponse().getStatus();
        assertThat(addPlayer).isIn(401, 403);
    }

    @Test
    void theAdminReachesTheHostsSide() throws Exception {
        String adminToken = jwtService.generateToken("admin", "ADMIN", 2L, "admin");
        mockMvc.perform(get("/api/admin/bday/guests").header("Authorization", "Bearer " + adminToken)).andExpect(status().isOk());
        mockMvc.perform(get("/api/admin/bday/questions").header("Authorization", "Bearer " + adminToken)).andExpect(status().isOk());
    }
}
