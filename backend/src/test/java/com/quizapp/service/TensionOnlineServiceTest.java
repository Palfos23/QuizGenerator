package com.quizapp.service;

import com.quizapp.dto.TensionAnswerEntryDto;
import com.quizapp.dto.TensionOnlineStateDto;
import com.quizapp.dto.TensionQuestionDto;
import com.quizapp.model.GameRoom;
import com.quizapp.model.RoomGameType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Online Tension fixes its whole question sequence when the room is created. The
// host can swap the CURRENT round's question for another one before anyone has
// answered - and the replacement must come from the same category filter the room
// was created with (which is now stored on the room state), never from the whole
// bank.
@SpringBootTest
class TensionOnlineServiceTest {

    @Autowired
    private RoomService roomService;
    @Autowired
    private TensionOnlineService tensionOnlineService;
    @Autowired
    private TensionQuestionService tensionQuestionService;

    private static final String HOST = "tension-host@example.com";
    private static final String GUEST = "tension-guest@example.com";

    private TensionAnswerEntryDto entry(int rank, String text) {
        TensionAnswerEntryDto e = new TensionAnswerEntryDto();
        e.setRank(rank);
        e.setText(text);
        return e;
    }

    private void createQuestion(String title, String category) {
        TensionQuestionDto dto = new TensionQuestionDto();
        dto.setTitle(title);
        dto.setMainCategory(category);
        dto.setSafeAnswers(List.of(entry(1, "Safe one")));
        dto.setTensionAnswers(List.of(entry(1, "Trap one")));
        tensionQuestionService.create(dto);
    }

    // 4 questions in the filtered category (2 get used, 2 stay available to swap
    // to) plus 3 in a different category that a swap must never pull from.
    private GameRoom startRoomFilteredTo(String category, String otherCategory) {
        for (int i = 0; i < 4; i++) createQuestion(category + " wanted " + i, category);
        for (int i = 0; i < 3; i++) createQuestion(otherCategory + " unwanted " + i, otherCategory);

        GameRoom room = roomService.createRoomShell(RoomGameType.TENSION, HOST, "Host", null);
        tensionOnlineService.initializeQuestionSequence(room, 2, category, List.of());
        roomService.join(room.getRoomCode(), GUEST, "Guest", null);
        room = roomService.findByCode(room.getRoomCode());
        tensionOnlineService.startGame(room, HOST);
        return roomService.findByCode(room.getRoomCode());
    }

    @Test
    void hostCanSwapTheQuestionAndItStaysInTheOriginalCategory() {
        String category = "SwapCat" + System.nanoTime();
        GameRoom room = startRoomFilteredTo(category, "Other" + System.nanoTime());
        String before = tensionOnlineService.getState(room, HOST).getQuestionTitle();

        TensionOnlineStateDto after = tensionOnlineService.rerollQuestion(room, HOST);

        assertThat(after.getQuestionTitle()).isNotEqualTo(before);
        assertThat(after.getQuestionTitle()).as("must still come from the room's own category").contains(category + " wanted");
    }

    @Test
    void onlyTheHostCanSwapTheQuestion() {
        GameRoom room = startRoomFilteredTo("SwapCat" + System.nanoTime(), "Other" + System.nanoTime());

        assertThatThrownBy(() -> tensionOnlineService.rerollQuestion(room, GUEST))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Only the host");
    }

    @Test
    void cannotSwapOnceSomeoneHasAnswered() {
        GameRoom room = startRoomFilteredTo("SwapCat" + System.nanoTime(), "Other" + System.nanoTime());
        tensionOnlineService.submitAnswer(room, HOST, "Safe one");

        assertThatThrownBy(() -> tensionOnlineService.rerollQuestion(room, HOST))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("too late");
    }
}
