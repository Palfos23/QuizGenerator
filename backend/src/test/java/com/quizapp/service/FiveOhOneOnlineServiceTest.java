package com.quizapp.service;

import com.quizapp.dto.FiveOhOneRoomCategoryDto;
import com.quizapp.model.FiveOhOneCategory;
import com.quizapp.model.RoomStatus;
import com.quizapp.model.GameRoom;
import com.quizapp.model.RoomGameType;
import com.quizapp.repository.FiveOhOneCategoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// 501's category is picked by the host when the room is created. While the room is
// still in the lobby the host can swap it for a different random one; once the game
// has started it's locked.
@SpringBootTest
class FiveOhOneOnlineServiceTest {

    @Autowired
    private RoomService roomService;
    @Autowired
    private FiveOhOneOnlineService fiveOhOneOnlineService;
    @Autowired
    private FiveOhOneCategoryRepository categoryRepository;

    private static final String HOST = "501-host@example.com";
    private static final String GUEST = "501-guest@example.com";

    private FiveOhOneCategory saveCategory(String title) {
        FiveOhOneCategory c = new FiveOhOneCategory();
        c.setTitle(title);
        return categoryRepository.save(c);
    }

    private GameRoom lobbyRoomWith(FiveOhOneCategory category) {
        GameRoom room = roomService.createRoomShell(RoomGameType.FIVE_O_ONE, HOST, "Host", null);
        fiveOhOneOnlineService.initializeCategory(room, category.getId());
        roomService.join(room.getRoomCode(), GUEST, "Guest", null);
        return roomService.findByCode(room.getRoomCode());
    }

    @Test
    void hostCanSwapToADifferentCategoryInTheLobby() {
        FiveOhOneCategory first = saveCategory("501 swap A " + System.nanoTime());
        saveCategory("501 swap B " + System.nanoTime());
        GameRoom room = lobbyRoomWith(first);
        assertThat(fiveOhOneOnlineService.getRoomCategory(room, HOST).getId()).isEqualTo(first.getId());

        FiveOhOneRoomCategoryDto swapped = fiveOhOneOnlineService.rerollCategory(room, HOST);

        assertThat(swapped.getId()).isNotEqualTo(first.getId());
        assertThat(fiveOhOneOnlineService.getRoomCategory(room, GUEST).getId()).isEqualTo(swapped.getId());
    }

    @Test
    void onlyTheHostCanSwapTheCategory() {
        FiveOhOneCategory first = saveCategory("501 swap A " + System.nanoTime());
        saveCategory("501 swap B " + System.nanoTime());
        GameRoom room = lobbyRoomWith(first);

        assertThatThrownBy(() -> fiveOhOneOnlineService.rerollCategory(room, GUEST))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Only the host");
    }

    @Test
    void cannotSwapOnceTheGameHasStarted() {
        FiveOhOneCategory first = saveCategory("501 swap A " + System.nanoTime());
        saveCategory("501 swap B " + System.nanoTime());
        GameRoom room = lobbyRoomWith(first);
        fiveOhOneOnlineService.startGame(room, HOST);
        GameRoom started = roomService.findByCode(room.getRoomCode());

        assertThatThrownBy(() -> fiveOhOneOnlineService.rerollCategory(started, HOST))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already started");
    }

    @Autowired
    private com.quizapp.repository.FiveOhOneRoomStateRepository roomStateRepository;

    // Regression: "Play again" did nothing. A game that ended by throws marked only its own round state
    // finished and left the room IN_PROGRESS, so RoomController#restart refused it ("This room hasn't
    // finished yet"). Both finishing paths in throwEntry now go through finishGame.
    @Test
    void finishingAGameLeavesTheRoomFinishedSoTheHostCanPlayAgain() {
        FiveOhOneCategory category = saveCategory("501 replay " + System.nanoTime());
        GameRoom room = lobbyRoomWith(category);
        fiveOhOneOnlineService.startGame(room, HOST);
        GameRoom running = roomService.findByCode(room.getRoomCode());
        assertThat(running.getStatus()).isEqualTo(RoomStatus.IN_PROGRESS);

        var state = roomStateRepository.findByRoom_Id(running.getId()).orElseThrow();
        fiveOhOneOnlineService.finishGame(running, state);

        GameRoom finishedRoom = roomService.findByCode(room.getRoomCode());
        assertThat(finishedRoom.getStatus()).isEqualTo(RoomStatus.FINISHED);
        assertThat(roomStateRepository.findByRoom_Id(finishedRoom.getId()).orElseThrow().isFinished()).isTrue();

        // ...which is what the restart endpoint requires: replay puts the room back in the lobby, same category.
        fiveOhOneOnlineService.restartForReplay(finishedRoom);
        GameRoom replay = roomService.markWaitingForReplay(finishedRoom);
        assertThat(replay.getStatus()).isEqualTo(RoomStatus.WAITING);
        assertThat(fiveOhOneOnlineService.getRoomCategory(replay, HOST).getId()).isEqualTo(category.getId());
    }
}
