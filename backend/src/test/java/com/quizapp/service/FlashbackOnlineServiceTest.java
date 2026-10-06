package com.quizapp.service;

import com.quizapp.dto.FlashbackOnlinePlayerDto;
import com.quizapp.dto.FlashbackOnlineStateDto;
import com.quizapp.dto.FlashbackYearRequest;
import com.quizapp.model.GameRoom;
import com.quizapp.model.RoomGameType;
import com.quizapp.repository.FlashbackRoomStateRepository;
import com.quizapp.repository.FlashbackYearRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Regression coverage: online Flashback used to leak a hint's in-progress
// guesses to everyone the instant any one player submitted, before the rest
// of the table had answered - a player could see what others guessed before
// guessing themselves. getState() now only exposes a hint's guesses once
// every active participant has answered it, same instant the round would
// naturally reveal that hint anyway. Also covers: there's no turn order -
// anyone can guess in any order - but a player still can't submit twice for
// the same hint.
@SpringBootTest
class FlashbackOnlineServiceTest {

    @Autowired
    private RoomService roomService;
    @Autowired
    private FlashbackOnlineService flashbackOnlineService;
    @Autowired
    private FlashbackAdminService flashbackAdminService;
    @Autowired
    private FlashbackRoomStateRepository flashbackRoomStateRepository;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private FlashbackYearRepository flashbackYearRepository;

    private static final String HOST = "flashback-host@example.com";
    private static final String GUEST = "flashback-guest@example.com";

    private GameRoom setUpTwoPlayerRoom() {
        FlashbackYearRequest request = new FlashbackYearRequest();
        request.setTitle("Guess-hiding test year " + System.nanoTime());
        request.setYear(1994);
        request.setHints(List.of("First hint", "Second hint", "Third hint"));
        flashbackAdminService.create(request);

        GameRoom room = roomService.createRoomShell(RoomGameType.FLASHBACK, HOST, "Host", null);
        flashbackOnlineService.initializeYearSequence(room, 1);
        roomService.join(room.getRoomCode(), GUEST, "Guest", null);
        room = roomService.findByCode(room.getRoomCode());
        flashbackOnlineService.startGame(room, HOST);
        return roomService.findByCode(room.getRoomCode());
    }

    @Test
    void otherPlayersGuessForTheCurrentHintStaysHiddenUntilEveryoneHasAnswered() {
        GameRoom room = setUpTwoPlayerRoom();

        // No turn order - the guest goes first here, on purpose, to prove
        // there's no "it's not your turn" gate blocking whoever wants to
        // answer first.
        flashbackOnlineService.submitGuess(room, GUEST, 2000);

        // The guest has answered, the host hasn't yet - from EITHER player's
        // point of view, the in-progress hint's guess must not be visible.
        FlashbackOnlineStateDto afterFirstGuess = flashbackOnlineService.getState(room, HOST);
        assertThat(afterFirstGuess.isRoundRevealed()).isFalse();
        assertThat(afterFirstGuess.getGuesses()).isEmpty();

        FlashbackOnlineStateDto afterFirstGuessOwnView = flashbackOnlineService.getState(room, GUEST);
        assertThat(afterFirstGuessOwnView.getGuesses())
                .as("even the guesser's own screen shouldn't see the other player's not-yet-submitted guess reflected back with data it doesn't have")
                .isEmpty();

        flashbackOnlineService.submitGuess(room, HOST, 1994);

        // Now that both have answered this hint, it's an exact match - the
        // round resolves and both guesses become visible together.
        FlashbackOnlineStateDto afterBothGuessed = flashbackOnlineService.getState(room, HOST);
        assertThat(afterBothGuessed.isRoundRevealed()).isTrue();
        assertThat(afterBothGuessed.getGuesses()).hasSize(2);
        assertThat(afterBothGuessed.getGuesses()).extracting(g -> g.getYear()).containsExactlyInAnyOrder(2000, 1994);
    }

    @Test
    void hasAnsweredCurrentHintFlagTracksEachPlayerIndependently() {
        GameRoom room = setUpTwoPlayerRoom();

        FlashbackOnlineStateDto before = flashbackOnlineService.getState(room, HOST);
        assertThat(before.getPlayers()).allMatch(p -> !p.isHasAnsweredCurrentHint());

        flashbackOnlineService.submitGuess(room, GUEST, 2000);

        FlashbackOnlineStateDto afterGuestGuess = flashbackOnlineService.getState(room, HOST);
        FlashbackOnlinePlayerDto guestDto = afterGuestGuess.getPlayers().stream()
                .filter(p -> p.getName().equals("Guest")).findFirst().orElseThrow();
        FlashbackOnlinePlayerDto hostDto = afterGuestGuess.getPlayers().stream()
                .filter(p -> p.getName().equals("Host")).findFirst().orElseThrow();
        assertThat(guestDto.isHasAnsweredCurrentHint()).isTrue();
        assertThat(hostDto.isHasAnsweredCurrentHint()).isFalse();
    }

    @Test
    void cannotSubmitTwiceForTheSameHint() {
        GameRoom room = setUpTwoPlayerRoom();

        flashbackOnlineService.submitGuess(room, GUEST, 2000);

        assertThatThrownBy(() -> flashbackOnlineService.submitGuess(room, GUEST, 2001))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already answered");
    }

    private Long currentYearId(GameRoom room) {
        // yearIds is a lazy collection - needs a session open while it's read
        return transactionTemplate.execute(status ->
                flashbackRoomStateRepository.findByRoom_Id(room.getId()).orElseThrow().getYearIds().get(0));
    }

    // Years added just so a swap has somewhere to go. They MUST be removed again afterwards: this
    // class's other tests start a room with initializeYearSequence, which draws a RANDOM year from
    // everything in the shared database and then assumes it's the 1994 one - so a stray extra year
    // makes those tests fail whenever it happens to be drawn.
    private final List<Long> extraYearIds = new java.util.ArrayList<>();

    @org.junit.jupiter.api.AfterEach
    void removeExtraYears() {
        flashbackYearRepository.deleteAllById(extraYearIds);
        extraYearIds.clear();
    }

    private void addAnotherYear() {
        FlashbackYearRequest request = new FlashbackYearRequest();
        request.setTitle("Swap-target year " + System.nanoTime());
        request.setYear(1986);
        request.setHints(List.of("Other first hint", "Other second hint"));
        extraYearIds.add(flashbackAdminService.create(request).getId());
    }

    @Test
    void hostCanSwapTheYearBeforeAnyoneGuesses() {
        GameRoom room = setUpTwoPlayerRoom();
        addAnotherYear();
        Long before = currentYearId(room);

        flashbackOnlineService.rerollYear(room, HOST);

        assertThat(currentYearId(room)).isNotEqualTo(before);
        Integer yearCount = transactionTemplate.execute(status ->
                flashbackRoomStateRepository.findByRoom_Id(room.getId()).orElseThrow().getYearIds().size());
        assertThat(yearCount).isEqualTo(1);
    }

    @Test
    void onlyTheHostCanSwapTheYear() {
        GameRoom room = setUpTwoPlayerRoom();
        addAnotherYear();

        assertThatThrownBy(() -> flashbackOnlineService.rerollYear(room, GUEST))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Only the host");
    }

    @Test
    void cannotSwapTheYearOnceSomeoneHasGuessed() {
        GameRoom room = setUpTwoPlayerRoom();
        addAnotherYear();
        flashbackOnlineService.submitGuess(room, GUEST, 2000);

        assertThatThrownBy(() -> flashbackOnlineService.rerollYear(room, HOST))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("too late");
    }
}
