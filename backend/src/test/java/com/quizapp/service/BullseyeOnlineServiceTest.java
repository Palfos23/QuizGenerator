package com.quizapp.service;

import com.quizapp.dto.BullseyeOnlineStateDto;
import com.quizapp.model.Athlete;
import com.quizapp.model.BullseyeEntry;
import com.quizapp.model.BullseyeQuestion;
import com.quizapp.model.GameRoom;
import com.quizapp.model.RoomGameType;
import com.quizapp.repository.AthleteRepository;
import com.quizapp.repository.BullseyeQuestionRepository;
import com.quizapp.repository.BullseyeRoomStateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Online Bullseye pre-picks its questions when the host starts the game. The host can
// swap the CURRENT round's question for a different, not-yet-used one before anyone
// has answered.
@SpringBootTest
class BullseyeOnlineServiceTest {

    @Autowired
    private RoomService roomService;
    @Autowired
    private BullseyeOnlineService bullseyeOnlineService;
    @Autowired
    private BullseyeQuestionRepository bullseyeQuestionRepository;
    @Autowired
    private BullseyeRoomStateRepository roomStateRepository;
    @Autowired
    private AthleteRepository athleteRepository;
    @Autowired
    private TransactionTemplate transactionTemplate;

    private static final String HOST = "bullseye-host@example.com";
    private static final String GUEST = "bullseye-guest@example.com";

    @BeforeEach
    void seedTwoEligibleQuestions() {
        // a swap needs at least one eligible question beyond the one in play
        saveQuestion("Swap Q1 " + System.nanoTime());
        saveQuestion("Swap Q2 " + System.nanoTime());
    }

    private void saveQuestion(String title) {
        String sport = "Football-" + System.nanoTime();
        Athlete a = new Athlete();
        a.setName("Bullseye Player A " + System.nanoTime());
        a.setSport(sport);
        a = athleteRepository.save(a);
        Athlete b = new Athlete();
        b.setName("Bullseye Player B " + System.nanoTime());
        b.setSport(sport);
        b = athleteRepository.save(b);

        BullseyeQuestion q = new BullseyeQuestion();
        q.setTitle(title);
        q.setSport(sport);
        q.setTargetValue(13);
        q.setStatLabel("goals");

        BullseyeEntry e1 = new BullseyeEntry();
        e1.setAthlete(a);
        e1.setStatValue(27);
        e1.setOrderIndex(0);
        BullseyeEntry e2 = new BullseyeEntry();
        e2.setAthlete(b);
        e2.setStatValue(18);
        e2.setOrderIndex(1);
        Set<BullseyeEntry> entries = new HashSet<>();
        entries.add(e1);
        entries.add(e2);
        q.setEntries(entries);
        bullseyeQuestionRepository.save(q);
    }

    private GameRoom startTwoPlayerRoom() {
        GameRoom room = roomService.createRoomShell(RoomGameType.BULLSEYE, HOST, "Host", null);
        roomService.join(room.getRoomCode(), GUEST, "Guest", null);
        room = roomService.findByCode(room.getRoomCode());
        bullseyeOnlineService.startGame(room, HOST);
        return roomService.findByCode(room.getRoomCode());
    }

    private Long currentQuestionId(GameRoom room) {
        // questionIds is a lazy collection - needs a session open while it's read
        return transactionTemplate.execute(status ->
                roomStateRepository.findByRoom_Id(room.getId()).orElseThrow().getQuestionIds().get(0));
    }

    @Test
    void hostCanSwapTheQuestionBeforeAnyoneAnswers() {
        GameRoom room = startTwoPlayerRoom();
        Long before = currentQuestionId(room);

        bullseyeOnlineService.rerollQuestion(room, HOST);

        assertThat(currentQuestionId(room)).isNotEqualTo(before);
    }

    @Test
    void onlyTheHostCanSwapTheQuestion() {
        GameRoom room = startTwoPlayerRoom();

        assertThatThrownBy(() -> bullseyeOnlineService.rerollQuestion(room, GUEST))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Only the host");
    }

    @Test
    void cannotSwapOnceSomeoneHasAnswered() {
        GameRoom room = startTwoPlayerRoom();
        BullseyeOnlineStateDto state = bullseyeOnlineService.getState(room, HOST);
        String firstEntryName = state.getEntries().get(0).getAthleteName();
        bullseyeOnlineService.submitAnswer(room, HOST, firstEntryName);

        assertThatThrownBy(() -> bullseyeOnlineService.rerollQuestion(room, HOST))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("too late");
    }
}
