package com.quizapp.service;

import com.quizapp.config.BdayQuestionCatalog;
import com.quizapp.dto.*;
import com.quizapp.repository.BdayAnswerRepository;
import com.quizapp.repository.BdayGuestRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class BdayQuizServiceTest {

    @Autowired private BdayQuizService service;
    @Autowired private BdayGuestRepository guestRepository;
    @Autowired private BdayAnswerRepository answerRepository;

    private final List<Long> createdQuestions = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        // the question table is shared with every other test - leave it as it was found
        createdQuestions.forEach(service::deleteQuestion);
        createdQuestions.clear();
    }

    private BdayGuestDto newGuest() {
        return service.addGuest("Guest " + System.nanoTime());
    }

    private BdayQuestionRequest request(String type, String prompt, String photoUrl, String option, String accepted) {
        BdayQuestionRequest r = new BdayQuestionRequest();
        r.setType(type);
        r.setPrompt(prompt);
        r.setPhotoUrl(photoUrl);
        r.setCorrectOption(option);
        r.setAcceptedAnswers(accepted);
        return r;
    }

    private BdayQuestionAdminDto create(BdayQuestionRequest request) {
        BdayQuestionAdminDto q = service.createQuestion(request);
        createdQuestions.add(q.getId());
        return q;
    }

    private BdaySubmitRequest.MapAnswer pinOnForde() {
        BdayQuestionCatalog.MapQuestion q = BdayQuestionCatalog.MAP_QUESTIONS.get(0);
        BdaySubmitRequest.MapAnswer a = new BdaySubmitRequest.MapAnswer();
        a.setQuestionId(q.id);
        a.setX(BdayQuestionCatalog.lngToX(q.correctLng));
        a.setY(BdayQuestionCatalog.latToY(q.correctLat));
        return a;
    }

    private BdaySubmitRequest.Answer answer(String questionKey, String value) {
        BdaySubmitRequest.Answer a = new BdaySubmitRequest.Answer();
        a.setQuestionId(questionKey);
        a.setValue(value);
        return a;
    }

    // Everything right: the pin on Førde, every question answered with its real answer.
    private BdaySubmitRequest perfectAnswers() {
        BdaySubmitRequest request = new BdaySubmitRequest();
        request.setMapAnswers(List.of(pinOnForde()));
        List<BdaySubmitRequest.Answer> answers = new ArrayList<>();
        for (BdayQuestionAdminDto q : service.listQuestions()) {
            String value = "TEXT".equals(q.getType()) ? q.getAcceptedAnswers().split("\n")[0] : q.getCorrectOption();
            answers.add(answer("q-" + q.getId(), value));
        }
        request.setAnswers(answers);
        return request;
    }

    @Test
    void theQuizStartsWithTheMapAndThenTheHostsQuestionsWithoutGivingAwayAnyAnswers() {
        BdayQuizDto quiz = service.getQuiz(newGuest().getId());

        assertThat(quiz.getMapImageUrl()).isEqualTo("/vestland-map.jpg");
        assertThat(quiz.getMapQuestions()).extracting(BdayQuizDto.MapQuestionDto::getPrompt).containsExactly("Hvor er Pål og Erik født?");
        // the five statements seeded the first time the table was empty
        assertThat(quiz.getQuestions()).extracting(BdayQuizDto.QuestionDto::getPrompt).contains(
                "Favorittlag: Middlesbrough", "Har vært i Japan", "Har vært i militæret",
                "Vart tatt ut på sonelaget i fotball", "Har flest mål i følge fotball.no");
        assertThat(quiz.getStatementOptions()).extracting(BdayQuizDto.OptionDto::getLabel).containsExactly("Pål", "Erik", "Begge", "Ingen");
        assertThat(quiz.getPhotoOptions()).extracting(BdayQuizDto.OptionDto::getLabel).containsExactly("Pål", "Erik");
    }

    @Test
    void questionsAreListedStatementsThenPhotosThenText() {
        create(request("TEXT", "Hvilken by?", null, null, "Bergen"));
        create(request("PHOTO", null, "https://example.com/a.jpg", "PAL", null));
        create(request("STATEMENT", "Har spist sushi", null, "BEGGE", null));

        List<String> types = service.getQuiz(newGuest().getId()).getQuestions().stream()
                .map(BdayQuizDto.QuestionDto::getType).toList();

        assertThat(types.lastIndexOf("STATEMENT")).isLessThan(types.indexOf("PHOTO"));
        assertThat(types.lastIndexOf("PHOTO")).isLessThan(types.indexOf("TEXT"));
    }

    @Test
    void aPerfectRunScoresEveryPoint() {
        create(request("STATEMENT", "Har spist sushi", null, "BEGGE", null));
        create(request("STATEMENT", "Har gått Birken", null, "INGEN", null));
        create(request("PHOTO", null, "https://example.com/a.jpg", "ERIK", null));
        create(request("TEXT", "Hvilken by er de født i?", null, null, "Førde\nForde"));
        BdayGuestDto guest = newGuest();

        BdayResultDto result = service.submitAnswers(guest.getId(), perfectAnswers());

        assertThat(result.getScore()).isEqualTo(service.maxTotalScore());
        assertThat(result.getMaxScore()).isEqualTo(BdayQuestionCatalog.MAP_POINTS + service.listQuestions().size());
        assertThat(result.getMapResults().get(0).isCorrect()).isTrue();
        assertThat(result.getSections()).extracting(BdayResultDto.SectionResult::getType).containsExactly("STATEMENT", "PHOTO", "TEXT");
        assertThat(result.getSections()).allMatch(s -> s.getCorrect() == s.getTotal());
    }

    @Test
    void everyOptionCanBeTheRightAnswerAndAWrongOneScoresNothing() {
        BdayQuestionAdminDto both = create(request("STATEMENT", "Begge har vært i Japan", null, "BEGGE", null));
        BdayQuestionAdminDto neither = create(request("STATEMENT", "Ingen har vært på Mars", null, "INGEN", null));
        BdayGuestDto guest = newGuest();

        BdaySubmitRequest request = new BdaySubmitRequest();
        request.setAnswers(List.of(answer("q-" + both.getId(), "BEGGE"), answer("q-" + neither.getId(), "PAL")));
        service.submitAnswers(guest.getId(), request);

        var rows = answerRepository.findByGuest_IdOrderByIdAsc(guest.getId());
        assertThat(rows).filteredOn(a -> a.getQuestionId().equals("q-" + both.getId())).allMatch(a -> a.getPointsEarned() == 1);
        assertThat(rows).filteredOn(a -> a.getQuestionId().equals("q-" + neither.getId())).allMatch(a -> a.getPointsEarned() == 0);
    }

    @Test
    void textAnswersIgnoreCaseAccentsAndPunctuation() {
        BdayQuestionAdminDto q = create(request("TEXT", "Hvilken by?", null, null, "Førde\nFørdefjorden"));

        assertThat(BdayQuizService.normalizeAnswer("  Førde!! ")).isEqualTo("forde");
        List<String> typed = List.of("forde", "FØRDEFJORDEN", "Bergen");
        List<Integer> points = new ArrayList<>();
        for (String text : typed) {
            BdayGuestDto guest = newGuest();
            BdaySubmitRequest r = new BdaySubmitRequest();
            r.setAnswers(List.of(answer("q-" + q.getId(), text)));
            service.submitAnswers(guest.getId(), r);
            points.add(answerRepository.findByGuest_IdOrderByIdAsc(guest.getId()).stream()
                    .filter(a -> a.getQuestionId().equals("q-" + q.getId())).mapToInt(a -> a.getPointsEarned()).sum());
        }
        assertThat(points).containsExactly(1, 1, 0);
    }

    @Test
    void theAdminCannotSaveHalfFinishedQuestions() {
        assertThatThrownBy(() -> service.createQuestion(request("STATEMENT", "", null, "PAL", null))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.createQuestion(request("STATEMENT", "Hei", null, "KANSKJE", null))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.createQuestion(request("PHOTO", null, "", "PAL", null))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.createQuestion(request("PHOTO", null, "https://x.no/a.jpg", "BEGGE", null))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.createQuestion(request("PHOTO", null, "javascript:alert(1)", "PAL", null))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.createQuestion(request("TEXT", "Spørsmål", null, null, "  \n "))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.createQuestion(request("QUIZ", "Hei", null, "PAL", null))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aQuestionCanBeEditedAndRemoved() {
        BdayQuestionAdminDto q = create(request("STATEMENT", "Gammel påstand", null, "PAL", null));

        BdayQuestionAdminDto edited = service.updateQuestion(q.getId(), request("STATEMENT", "Ny påstand", null, "INGEN", null));

        assertThat(edited.getPrompt()).isEqualTo("Ny påstand");
        assertThat(edited.getCorrectOption()).isEqualTo("INGEN");
        service.deleteQuestion(q.getId());
        createdQuestions.remove(q.getId());
        assertThat(service.listQuestions()).noneMatch(x -> x.getId().equals(q.getId()));
    }

    @Test
    void thePhotoQuestionShowsItsPhotoToGuestsButNeverTheAnswer() {
        BdayQuestionAdminDto q = create(request("PHOTO", null, "https://example.com/barn.jpg", "ERIK", null));

        var shown = service.getQuiz(newGuest().getId()).getQuestions().stream()
                .filter(x -> x.getId().equals("q-" + q.getId())).findFirst().orElseThrow();

        assertThat(shown.getPhotoUrl()).isEqualTo("https://example.com/barn.jpg");
        assertThat(shown.getPrompt()).isEqualTo("Hvem er på bildet?");
        assertThat(shown.getClass().getDeclaredFields()).noneMatch(f -> f.getName().toLowerCase().contains("correct"));
    }

    @Test
    void theHostCanSeeAFinishedGuestsAnswerSheet() {
        create(request("TEXT", "Hvilken by?", null, null, "Bergen"));
        BdayGuestDto guest = newGuest();
        service.submitAnswers(guest.getId(), perfectAnswers());

        var sheet = service.guestAnswers(guest.getId());

        assertThat(sheet).extracting(BdayAnswerViewDto::getType).contains("MAP", "STATEMENT", "TEXT");
        assertThat(sheet).filteredOn(a -> a.getType().equals("TEXT")).extracting(BdayAnswerViewDto::getResponse).contains("Bergen");
        assertThat(sheet).filteredOn(a -> a.getType().equals("STATEMENT")).extracting(BdayAnswerViewDto::getResponse)
                .allMatch(r -> List.of("Pål", "Erik", "Begge", "Ingen").contains(r));
    }

    @Test
    void theHostCanMarkAnAnswerRightOrWrongAndTheTotalFollows() {
        create(request("TEXT", "Hvilken by?", null, null, "Bergen"));
        BdayGuestDto guest = newGuest();
        service.submitAnswers(guest.getId(), perfectAnswers());
        int full = guestRepository.findById(guest.getId()).orElseThrow().getScore();

        // a right text answer the automatic check missed ... or one it let through that shouldn't count
        var text = service.guestAnswers(guest.getId()).stream().filter(a -> a.getType().equals("TEXT")).findFirst().orElseThrow();
        assertThat(text.getCorrectAnswer()).isEqualTo("Bergen");
        service.markAnswer(guest.getId(), text.getId(), false);
        assertThat(guestRepository.findById(guest.getId()).orElseThrow().getScore()).isEqualTo(full - 1);

        var after = service.markAnswer(guest.getId(), text.getId(), true);
        assertThat(guestRepository.findById(guest.getId()).orElseThrow().getScore()).isEqualTo(full);
        assertThat(after).filteredOn(a -> a.getId().equals(text.getId())).allMatch(a -> a.getPoints() == a.getMaxPoints());

        // the map question is worth more than a point
        var map = service.guestAnswers(guest.getId()).stream().filter(a -> a.getType().equals("MAP")).findFirst().orElseThrow();
        service.markAnswer(guest.getId(), map.getId(), false);
        assertThat(guestRepository.findById(guest.getId()).orElseThrow().getScore()).isEqualTo(full - map.getMaxPoints());
    }

    @Test
    void anAnswerBelongingToSomeoneElseCannotBeMarked() {
        BdayGuestDto a = newGuest();
        BdayGuestDto b = newGuest();
        service.submitAnswers(a.getId(), perfectAnswers());
        service.submitAnswers(b.getId(), perfectAnswers());
        Long bsAnswer = service.guestAnswers(b.getId()).get(0).getId();

        assertThatThrownBy(() -> service.markAnswer(a.getId(), bsAnswer, false)).isInstanceOf(RuntimeException.class);
    }

    @Test
    void waitingPlayersAreListedAlphabetically() {
        service.addGuest("Zeta " + System.nanoTime());
        service.addGuest("Alfa " + System.nanoTime());

        List<String> names = service.listPendingGuests().stream().map(BdayGuestDto::getName).toList();

        assertThat(names).isSortedAccordingTo(String.CASE_INSENSITIVE_ORDER);
    }

    @Test
    void theHostCanChangeAFinishedGuestsScoreWithinTheValidRange() {
        BdayGuestDto guest = newGuest();
        service.submitAnswers(guest.getId(), perfectAnswers());

        assertThat(service.setScore(guest.getId(), 7).getScore()).isEqualTo(7);
        assertThat(guestRepository.findById(guest.getId()).orElseThrow().getScore()).isEqualTo(7);
        assertThat(service.leaderboard()).anyMatch(g -> g.getId().equals(guest.getId()) && g.getScore() == 7);

        assertThatThrownBy(() -> service.setScore(guest.getId(), -1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.setScore(guest.getId(), service.maxTotalScore() + 1)).isInstanceOf(IllegalArgumentException.class);
        // A guest who hasn't played has no score to change.
        BdayGuestDto waiting = newGuest();
        assertThatThrownBy(() -> service.setScore(waiting.getId(), 3)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void resettingAFinishedGuestLetsThemPlayAgainAndClearsTheirAnswers() {
        BdayGuestDto guest = newGuest();
        service.submitAnswers(guest.getId(), perfectAnswers());
        assertThat(answerRepository.findAll()).anyMatch(a -> a.getGuest().getId().equals(guest.getId()));
        assertThatThrownBy(() -> service.getQuiz(guest.getId())).isInstanceOf(IllegalStateException.class);

        BdayGuestDto reset = service.resetGuest(guest.getId());

        assertThat(reset.getStatus()).isEqualTo("PENDING");
        assertThat(reset.getScore()).isZero();
        assertThat(reset.getCompletedAt()).isNull();
        assertThat(answerRepository.findAll()).noneMatch(a -> a.getGuest().getId().equals(guest.getId()));
        assertThat(service.leaderboard()).noneMatch(g -> g.getId().equals(guest.getId()));
        assertThat(service.listPendingGuests()).anyMatch(g -> g.getId().equals(guest.getId()));
        // ...and they really can take the quiz again.
        assertThat(service.submitAnswers(guest.getId(), perfectAnswers()).getScore()).isEqualTo(service.maxTotalScore());
        // Resetting someone who hasn't finished is harmless.
        assertThat(service.resetGuest(newGuest().getId()).getStatus()).isEqualTo("PENDING");
    }
}
