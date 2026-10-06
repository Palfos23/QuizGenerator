package com.quizapp.service;

import com.quizapp.dto.ClientEventRequest;
import com.quizapp.dto.GridCategoryDto;
import com.quizapp.dto.GridCategoryRequest;
import com.quizapp.dto.TensionAnswerEntryDto;
import com.quizapp.dto.TensionQuestionDto;
import com.quizapp.model.Athlete;
import com.quizapp.repository.AthleteRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Regression: players got NO suggestions on a Tension question whose answers come from
// Subjects, so couldn't answer. A guest (someone who joined a room by code/QR with no
// account) was refused by GET /api/tension/questions/subject-options - the security rules
// let guests fetch the category-based list but not this one - and the access check behind
// it also looks the caller up as an account, which a guest isn't. Also covers the
// logging/resilience added around it.
@SpringBootTest
@AutoConfigureMockMvc
class TensionSubjectOptionsTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private AuthService authService;
    @Autowired
    private AthleteRepository athleteRepository;
    @Autowired
    private TensionQuestionService tensionQuestionService;
    @Autowired
    private GridCategoryService gridCategoryService;
    @Autowired
    private ClientEventLogService clientEventLogService;

    private void addAthlete(String sport, String name) {
        Athlete a = new Athlete();
        a.setName(name);
        a.setSport(sport);
        athleteRepository.save(a);
    }

    private String guestToken() {
        return authService.loginAsGuest("Guest", "test-" + System.nanoTime()).getToken();
    }

    private String uniqueSport() {
        return "SubjectsTest" + System.nanoTime();
    }

    @Test
    void aGuestCanFetchTheSubjectSuggestionsOverHttp() throws Exception {
        String sport = uniqueSport();
        addAthlete(sport, "Alexander Isak");
        addAthlete(sport, "Florian Wirtz");

        mockMvc.perform(get("/api/tension/questions/subject-options")
                        .param("sport", sport)
                        .header("Authorization", "Bearer " + guestToken()))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Alexander Isak")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Florian Wirtz")));
    }

    @Test
    void anonymousCallersStillCannotFetchIt() throws Exception {
        mockMvc.perform(get("/api/tension/questions/subject-options").param("sport", uniqueSport()))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void namesComeBackSortedAndAMismatchedCaseOrWhitespaceStillFindsThem() {
        String sport = uniqueSport();
        addAthlete(sport, "Zeta Player");
        addAthlete(sport, "Alpha Player");

        assertThat(tensionQuestionService.getSubjectOptions(sport)).containsExactly("Alpha Player", "Zeta Player");
        // A question that stores the name with different case / stray spaces must still work.
        assertThat(tensionQuestionService.getSubjectOptions("  " + sport.toUpperCase() + " "))
                .containsExactly("Alpha Player", "Zeta Player");
    }

    @Test
    void anUnknownSportIsEmptyAndNotCachedSoItIsReCheckedNextTime() {
        String sport = uniqueSport();
        assertThat(tensionQuestionService.getSubjectOptions(sport)).isEmpty();

        addAthlete(sport, "Late Arrival");

        assertThat(tensionQuestionService.getSubjectOptions(sport)).containsExactly("Late Arrival");
    }

    @Test
    void aSuccessfulListIsCachedBrieflyToSpareTheDatabase() {
        String sport = uniqueSport();
        addAthlete(sport, "First Player");
        assertThat(tensionQuestionService.getSubjectOptions(sport)).containsExactly("First Player");

        addAthlete(sport, "Second Player");

        assertThat(tensionQuestionService.getSubjectOptions(sport)).as("served from the short-lived cache").containsExactly("First Player");
    }

    @Test
    void renamingASubjectsCategoryAlsoRenamesTensionQuestionsUsingIt() {
        String oldName = uniqueSport();
        GridCategoryRequest create = new GridCategoryRequest();
        create.setName(oldName);
        GridCategoryDto category = gridCategoryService.create(create);

        TensionAnswerEntryDto safe = new TensionAnswerEntryDto();
        safe.setRank(1);
        safe.setText("Someone");
        TensionAnswerEntryDto trap = new TensionAnswerEntryDto();
        trap.setRank(1);
        trap.setText("Trap");
        TensionQuestionDto question = new TensionQuestionDto();
        question.setTitle("Rename cascade test " + System.nanoTime());
        question.setMainCategory("Test");
        question.setAnswersFromSubjects(true);
        question.setAnswersSport(oldName);
        question.setSafeAnswers(List.of(safe));
        question.setTensionAnswers(List.of(trap));
        Long questionId = tensionQuestionService.create(question).getId();

        String newName = oldName + " Renamed";
        GridCategoryRequest rename = new GridCategoryRequest();
        rename.setName(newName);
        gridCategoryService.update(category.getId(), rename);

        assertThat(tensionQuestionService.getOne(questionId).getAnswersSport())
                .as("otherwise its answer box would look up athletes under a name that no longer exists")
                .isEqualTo(newName);
    }

    // ---- client-side problem reports ----

    @Test
    void aGuestCanReportASuggestionProblemAndGetsNoContentBack() throws Exception {
        mockMvc.perform(post("/api/diagnostics/client-event")
                        .header("Authorization", "Bearer " + guestToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"area\":\"tension-suggestions\",\"kind\":\"FETCH_FAILED\",\"key\":\"Football\",\"httpStatus\":403,\"detail\":\"boom\"}"))
                .andExpect(status().isNoContent());
    }

    @Test
    void reportsRequireASignedInCaller() throws Exception {
        mockMvc.perform(post("/api/diagnostics/client-event")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"area\":\"a\",\"kind\":\"b\"}"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void oneCallerCannotFloodTheLogAndEmbeddedNewlinesAreFlattened() {
        ClientEventRequest event = new ClientEventRequest();
        event.setArea("tension-suggestions");
        event.setKind("FETCH_FAILED");
        event.setDetail("line one\nFORGED LOG LINE\r\nmore");
        String caller = "flood-test-" + System.nanoTime();

        int accepted = 0;
        for (int i = 0; i < ClientEventLogService.MAX_EVENTS_PER_WINDOW + 10; i++) {
            if (clientEventLogService.record(caller, "GUEST", event, "test-agent")) accepted++;
        }

        assertThat(accepted).isEqualTo(ClientEventLogService.MAX_EVENTS_PER_WINDOW);
        assertThat(ClientEventLogService.clean(event.getDetail())).doesNotContain("\n").doesNotContain("\r");
    }
}
