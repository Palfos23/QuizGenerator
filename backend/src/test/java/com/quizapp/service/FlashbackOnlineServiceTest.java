package com.quizapp.service;

import com.quizapp.dto.FlashbackOnlineStateDto;
import com.quizapp.dto.FlashbackYearRequest;
import com.quizapp.model.GameRoom;
import com.quizapp.model.RoomGameType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// Regression coverage: online Flashback used to leak a hint's in-progress
// guesses to everyone the instant any one player submitted, before the rest
// of the table had answered - a player could see what others guessed before
// guessing themselves. getState() now only exposes a hint's guesses once
// every active participant has answered it, same instant the round would
// naturally reveal that hint anyway.
@SpringBootTest
class FlashbackOnlineServiceTest {

    @Autowired
    private RoomService roomService;
    @Autowired
    private FlashbackOnlineService flashbackOnlineService;
    @Autowired
    private FlashbackAdminService flashbackAdminService;

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

        FlashbackOnlineStateDto beforeAnyGuess = flashbackOnlineService.getState(room, HOST);
        assertThat(beforeAnyGuess.getCurrentTurnParticipantId()).isNotNull();
        boolean hostGoesFirst = beforeAnyGuess.getCurrentTurnParticipantId().equals(beforeAnyGuess.getYourParticipantId());
        String firstEmail = hostGoesFirst ? HOST : GUEST;
        String secondEmail = hostGoesFirst ? GUEST : HOST;

        flashbackOnlineService.submitGuess(room, firstEmail, 2000);

        // The first player has answered, the second hasn't yet - from
        // EITHER player's point of view, the in-progress hint's guess must
        // not be visible.
        FlashbackOnlineStateDto afterFirstGuess = flashbackOnlineService.getState(room, secondEmail);
        assertThat(afterFirstGuess.isRoundRevealed()).isFalse();
        assertThat(afterFirstGuess.getGuesses()).isEmpty();

        FlashbackOnlineStateDto afterFirstGuessOwnView = flashbackOnlineService.getState(room, firstEmail);
        assertThat(afterFirstGuessOwnView.getGuesses())
                .as("even the guesser's own screen shouldn't see the other player's not-yet-submitted guess reflected back with data it doesn't have")
                .isEmpty();

        flashbackOnlineService.submitGuess(room, secondEmail, 1994);

        // Now that both have answered this hint, it's an exact match - the
        // round resolves and both guesses become visible together.
        FlashbackOnlineStateDto afterBothGuessed = flashbackOnlineService.getState(room, HOST);
        assertThat(afterBothGuessed.isRoundRevealed()).isTrue();
        assertThat(afterBothGuessed.getGuesses()).hasSize(2);
        assertThat(afterBothGuessed.getGuesses()).extracting(g -> g.getYear()).containsExactlyInAnyOrder(2000, 1994);
    }
}
