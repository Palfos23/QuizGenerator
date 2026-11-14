package com.quizapp.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import com.quizapp.dto.DailyQuizPlayStateDto;
import com.quizapp.service.DailyQuizService;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

// A visitor without an account may read today's questions (the landing page's "play today's quiz"),
// and nothing more: every other daily quiz call - handing in answers above all - still needs an account.
@SpringBootTest
@AutoConfigureMockMvc
class DailyQuizVisitorAccessTest {

    @Autowired
    private MockMvc mockMvc;

    // Mocked so this test never generates today's real set: DailyQuizServiceTest seeds its own questions
    // first and expects to be the one that creates it. What's under test here is who gets through the door.
    @MockBean
    private DailyQuizService dailyQuizService;

    @Test
    void aVisitorCanReadTodaysQuestions() throws Exception {
        DailyQuizPlayStateDto dto = new DailyQuizPlayStateDto();
        dto.setAttemptStatus("IN_PROGRESS");
        org.mockito.Mockito.when(dailyQuizService.getTodayForVisitor()).thenReturn(dto);

        var response = mockMvc.perform(get("/api/daily-quiz/today")).andReturn().getResponse();
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getContentAsString()).contains("\"attemptStatus\":\"IN_PROGRESS\"");
    }

    @Test
    void everythingElseStillNeedsAnAccount() throws Exception {
        assertThat(mockMvc.perform(get("/api/daily-quiz/active")).andReturn().getResponse().getStatus()).isIn(401, 403);
        assertThat(mockMvc.perform(get("/api/daily-quiz/1/play")).andReturn().getResponse().getStatus()).isIn(401, 403);
        assertThat(mockMvc.perform(post("/api/daily-quiz/1/submit").contentType("application/json").content("{\"answers\":[]}"))
                .andReturn().getResponse().getStatus()).isIn(401, 403);
        assertThat(mockMvc.perform(post("/api/daily-quiz/today")).andReturn().getResponse().getStatus()).isIn(401, 403);
    }
}
