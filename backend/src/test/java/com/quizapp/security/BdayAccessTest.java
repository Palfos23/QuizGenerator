package com.quizapp.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// The birthday quiz has two sides: guests play on their own phones with no login and no PIN, and the host's side
// (players, questions, answers) needs the quiz's own PIN. The site's admin login deliberately does NOT open it -
// some admins are guests at the party, and the host's side shows every answer.
@SpringBootTest
@AutoConfigureMockMvc
class BdayAccessTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtService jwtService;
    @Value("${app.birthday.password}")
    private String pin;

    // Each request pretends to come from its own address: repeated wrong PINs lock a client out (by design), and
    // these tests deliberately make many wrong attempts.
    private int next = 0;

    private String ip() {
        return "10.9.8." + (next++ % 250);
    }

    @Test
    void guestsReachTheQuizWithoutLoggingInOrAnyPin() throws Exception {
        mockMvc.perform(get("/api/bday/guests")).andExpect(status().isOk());
        mockMvc.perform(get("/api/bday/leaderboard")).andExpect(status().isOk());
        mockMvc.perform(get("/api/bday/standings")).andExpect(status().isOk());
        mockMvc.perform(get("/api/bday/mariokart")).andExpect(status().isOk());
    }

    @Test
    void theHostsSideNeedsThePin() throws Exception {
        for (String path : new String[] {"/api/bday/admin/guests", "/api/bday/admin/questions", "/api/bday/admin/settings",
                "/api/bday/admin/mariokart", "/api/bday/admin/teams", "/api/bday/admin/wheel", "/api/bday/admin/standings", "/api/bday/admin/reveal"}) {
            mockMvc.perform(get(path).header("X-Forwarded-For", ip())).andExpect(status().isUnauthorized());
            mockMvc.perform(get(path).header("X-Bday-Pin", "not-the-pin").header("X-Forwarded-For", ip())).andExpect(status().isUnauthorized());
            mockMvc.perform(get(path).header("X-Bday-Pin", pin).header("X-Forwarded-For", ip())).andExpect(status().isOk());
        }
    }

    @Test
    void beingASiteAdminDoesNotOpenTheHostsSide() throws Exception {
        String adminToken = jwtService.generateToken("admin", "ADMIN", 2L, "admin");
        mockMvc.perform(get("/api/bday/admin/questions").header("Authorization", "Bearer " + adminToken).header("X-Forwarded-For", ip()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/bday/admin/guests/1/answers").header("Authorization", "Bearer " + adminToken).header("X-Forwarded-For", ip()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void aStrangerCannotAddPlayersOrQuestions() throws Exception {
        mockMvc.perform(post("/api/bday/admin/guests").header("X-Forwarded-For", ip()).contentType("application/json").content("{\"name\":\"Hacker\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/bday/admin/wheel/pick").header("X-Forwarded-For", ip())).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/bday/admin/teams").header("X-Forwarded-For", ip()).contentType("application/json").content("{\"name\":\"Hackerlaget\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(multipart("/api/bday/admin/images").file("file", new byte[] {1, 2, 3}).header("X-Forwarded-For", ip()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/bday/admin/questions").header("X-Forwarded-For", ip()).contentType("application/json").content("{\"type\":\"STATEMENT\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void theOldAdminPathIsGone() throws Exception {
        // the host's endpoints used to live under /api/admin/bday behind the admin role - nothing may answer there any more
        String adminToken = jwtService.generateToken("admin", "ADMIN", 2L, "admin");
        int status = mockMvc.perform(get("/api/admin/bday/questions").header("Authorization", "Bearer " + adminToken))
                .andReturn().getResponse().getStatus();
        org.assertj.core.api.Assertions.assertThat(status).isGreaterThanOrEqualTo(400);
    }
}
