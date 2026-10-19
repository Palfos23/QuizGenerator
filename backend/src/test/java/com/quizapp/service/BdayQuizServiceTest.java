package com.quizapp.service;

import com.quizapp.config.BdayQuestionCatalog;
import com.quizapp.dto.BdayGuestDto;
import com.quizapp.dto.BdayQuizDto;
import com.quizapp.dto.BdayResultDto;
import com.quizapp.dto.BdaySubmitRequest;
import com.quizapp.repository.BdayAnswerRepository;
import com.quizapp.repository.BdayGuestRepository;
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

    private BdayGuestDto newGuest() {
        return service.addGuest("Guest " + System.nanoTime());
    }

    // Everything right: the map pin on the correct city, every statement assigned to its real owner.
    private BdaySubmitRequest perfectAnswers() {
        BdaySubmitRequest request = new BdaySubmitRequest();
        List<BdaySubmitRequest.MapAnswer> maps = new ArrayList<>();
        for (BdayQuestionCatalog.MapQuestion q : BdayQuestionCatalog.MAP_QUESTIONS) {
            BdaySubmitRequest.MapAnswer a = new BdaySubmitRequest.MapAnswer();
            a.setQuestionId(q.id);
            a.setX((q.correctLng - BdayQuestionCatalog.MAP_LEFT_LNG) / (BdayQuestionCatalog.MAP_RIGHT_LNG - BdayQuestionCatalog.MAP_LEFT_LNG));
            a.setY((BdayQuestionCatalog.MAP_TOP_LAT - q.correctLat) / (BdayQuestionCatalog.MAP_TOP_LAT - BdayQuestionCatalog.MAP_BOTTOM_LAT));
            maps.add(a);
        }
        request.setMapAnswers(maps);
        List<BdaySubmitRequest.TileAnswer> tiles = new ArrayList<>();
        BdayQuestionCatalog.TILE_QUESTIONS.forEach(q -> q.tiles.forEach(t -> {
            BdaySubmitRequest.TileAnswer a = new BdaySubmitRequest.TileAnswer();
            a.setTileId(t.id);
            a.setChosenOwner(t.correctOwner);
            tiles.add(a);
        }));
        request.setTileAnswers(tiles);
        return request;
    }

    @Test
    void theQuizIsInNorwegianWithPalAndErikAndFiveStatementsAfterTheMapQuestion() {
        BdayQuizDto quiz = service.getQuiz(newGuest().getId());

        assertThat(quiz.getMapQuestions()).hasSize(1);
        assertThat(quiz.getMapQuestions().get(0).getPrompt()).isEqualTo("Hvor er Pål og Erik født?");
        assertThat(quiz.getTileQuestions()).hasSize(1);
        var statements = quiz.getTileQuestions().get(0);
        assertThat(statements.getPersonALabel()).isEqualTo("Pål");
        assertThat(statements.getPersonBLabel()).isEqualTo("Erik");
        assertThat(statements.getTiles()).extracting(BdayQuizDto.TileDto::getLabel).containsExactly(
                "Favorittlag: Middlesbrough", "Har vært i Japan", "Har vært i militæret",
                "Vart tatt ut på sonelaget i fotball", "Har flest mål i følge fotball.no");
        // Erik, Pål, Erik, Erik, Pål - the owners are never sent to the guest, only used when scoring.
        assertThat(BdayQuestionCatalog.TILE_QUESTIONS.get(0).tiles).extracting(t -> t.correctOwner)
                .containsExactly("B", "A", "B", "B", "A");
    }

    @Test
    void aPerfectRunScoresEveryPoint() {
        BdayGuestDto guest = newGuest();

        BdayResultDto result = service.submitAnswers(guest.getId(), perfectAnswers());

        assertThat(result.getScore()).isEqualTo(BdayQuestionCatalog.maxTotalScore());
        assertThat(result.getMaxScore()).isEqualTo(15); // 10 for the map + 5 statements
    }

    @Test
    void theHostCanChangeAFinishedGuestsScoreWithinTheValidRange() {
        BdayGuestDto guest = newGuest();
        service.submitAnswers(guest.getId(), perfectAnswers());

        assertThat(service.setScore(guest.getId(), 7).getScore()).isEqualTo(7);
        assertThat(guestRepository.findById(guest.getId()).orElseThrow().getScore()).isEqualTo(7);
        assertThat(service.leaderboard()).anyMatch(g -> g.getId().equals(guest.getId()) && g.getScore() == 7);

        assertThatThrownBy(() -> service.setScore(guest.getId(), -1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.setScore(guest.getId(), BdayQuestionCatalog.maxTotalScore() + 1))
                .isInstanceOf(IllegalArgumentException.class);
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
        assertThat(service.submitAnswers(guest.getId(), perfectAnswers()).getScore()).isEqualTo(BdayQuestionCatalog.maxTotalScore());
        // Resetting someone who hasn't finished is harmless.
        assertThat(service.resetGuest(newGuest().getId()).getStatus()).isEqualTo("PENDING");
    }
}
