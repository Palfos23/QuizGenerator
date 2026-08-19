package com.quizapp.service;

import com.quizapp.dto.GridBattleStateDto;
import com.quizapp.model.Athlete;
import com.quizapp.model.GameRoom;
import com.quizapp.model.Grid;
import com.quizapp.model.GridEntry;
import com.quizapp.model.RoomGameType;
import com.quizapp.repository.AthleteRepository;
import com.quizapp.repository.GridRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Exercises "Random" Grid Battle's new round-choice picker end to end through
// the service layer: room creation no longer resolves the whole grid sequence
// up front, getState() offers 3 live choices to whoever starts each round, only
// that participant can pick, and advancing rotates a fresh choice for the next
// round while excluding whatever's already been played.
@SpringBootTest
class GridBattleOnlineServiceTest {

    @Autowired
    private RoomService roomService;
    @Autowired
    private GridBattleOnlineService gridBattleOnlineService;
    @Autowired
    private GridRepository gridRepository;
    @Autowired
    private AthleteRepository athleteRepository;

    private static final String HOST = "host@example.com";
    private static final String GUEST = "guest@example.com";

    @BeforeEach
    void seedEnoughGridsToChooseFrom() {
        // getBattleRoundChoices offers 3 candidates per round - need enough
        // distinct grids in the pool that a real choice (and a later, different
        // round-2 choice excluding round 1's pick) is actually possible.
        for (int i = 0; i < 6; i++) {
            saveGridWithOneEntry("Battle Grid " + System.nanoTime() + "-" + i);
        }
    }

    private Grid saveGridWithOneEntry(String title) {
        Athlete a = new Athlete();
        a.setName("Player " + System.nanoTime());
        a.setSport("Football");
        a = athleteRepository.save(a);

        Grid g = new Grid();
        g.setTitle(title);
        g.setSport("Football");
        g.setWeekStartDate(LocalDate.now());
        g.setMaxStrikes(3);

        GridEntry e = new GridEntry();
        e.setAthlete(a);
        e.setOrderIndex(0);
        Set<GridEntry> entries = new HashSet<>();
        entries.add(e);
        g.setEntries(entries);

        return gridRepository.save(g);
    }

    private GameRoom setUpTwoPlayerRandomRoom(int rounds) {
        GameRoom room = roomService.createRoomShell(RoomGameType.GRID_BATTLE, HOST, "Host", null);
        gridBattleOnlineService.initializeGridSequence(room, null, rounds);
        roomService.join(room.getRoomCode(), GUEST, "Guest", null);
        // join() persists the new participant via its own re-fetch of the room -
        // this test's own "room" reference is now stale (still shows 1 participant)
        // and needs re-fetching before startGame() can see both players.
        room = roomService.findByCode(room.getRoomCode());
        gridBattleOnlineService.startGame(room, HOST);
        return room;
    }

    @Test
    void randomRoomOffersThreeChoicesToTheFirstPicker() {
        GameRoom room = setUpTwoPlayerRandomRoom(2);
        GridBattleStateDto state = gridBattleOnlineService.getState(room, HOST);

        assertThat(state.isAwaitingGridChoice()).isTrue();
        assertThat(state.getGridChoices()).hasSize(3);
        assertThat(state.getTotalGrids()).isEqualTo(2);
        assertThat(state.getCurrentGridIndex()).isZero();
        // Round 0's starter is participant index 0 % 2 == the host (who joined first).
        Long hostParticipantId = state.getYourParticipantId();
        assertThat(state.getPickerParticipantId()).isEqualTo(hostParticipantId);
    }

    @Test
    void repeatedPollsOfferTheSameThreeChoicesUntilPicked() {
        GameRoom room = setUpTwoPlayerRandomRoom(2);
        GridBattleStateDto first = gridBattleOnlineService.getState(room, HOST);
        GridBattleStateDto second = gridBattleOnlineService.getState(room, HOST);

        List<Long> firstIds = first.getGridChoices().stream().map(g -> g.getId()).sorted().toList();
        List<Long> secondIds = second.getGridChoices().stream().map(g -> g.getId()).sorted().toList();
        assertThat(secondIds).isEqualTo(firstIds);
    }

    @Test
    void onlyThePickerCanChooseAndOnlyFromTheOfferedOptions() {
        GameRoom room = setUpTwoPlayerRandomRoom(2);
        GridBattleStateDto state = gridBattleOnlineService.getState(room, HOST);
        Long offeredId = state.getGridChoices().get(0).getId();

        assertThatThrownBy(() -> gridBattleOnlineService.chooseGrid(room, GUEST, offeredId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not your turn");

        Grid notOffered = gridRepository.findAll().stream()
                .filter(g -> state.getGridChoices().stream().noneMatch(c -> c.getId().equals(g.getId())))
                .findFirst().orElseThrow();
        assertThatThrownBy(() -> gridBattleOnlineService.chooseGrid(room, HOST, notOffered.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("offered");
    }

    @Test
    void choosingAGridStartsTheRoundAndFinishingItOffersAFreshChoiceExcludingThePrevious() {
        GameRoom room = setUpTwoPlayerRandomRoom(2);
        GridBattleStateDto beforePick = gridBattleOnlineService.getState(room, HOST);
        Long firstGridId = beforePick.getGridChoices().get(0).getId();

        GridBattleStateDto afterPick = gridBattleOnlineService.chooseGrid(room, HOST, firstGridId);
        assertThat(afterPick.isAwaitingGridChoice()).isFalse();
        assertThat(afterPick.getCurrentGridId()).isEqualTo(firstGridId);
        assertThat(afterPick.getEntries()).hasSize(1);

        // Solve the grid's one entry to complete it, then advance as host.
        Long athleteId = gridRepository.findById(firstGridId).orElseThrow()
                .getEntries().iterator().next().getAthlete().getId();
        GridBattleStateDto afterGuess = gridBattleOnlineService.guess(room, HOST, athleteId);
        assertThat(afterGuess.isGridComplete()).isTrue();

        GridBattleStateDto round2 = gridBattleOnlineService.advanceToNextGrid(room, HOST);
        assertThat(round2.isFinished()).isFalse();
        assertThat(round2.getCurrentGridIndex()).isEqualTo(1);
        assertThat(round2.isAwaitingGridChoice()).isTrue();
        assertThat(round2.getGridChoices()).extracting(g -> g.getId()).doesNotContain(firstGridId);
        // Round 1 (index 1) starter is participant index 1 % 2 == the guest.
        Long guestParticipantId = gridBattleOnlineService.getState(room, GUEST).getYourParticipantId();
        assertThat(round2.getPickerParticipantId()).isEqualTo(guestParticipantId);
    }

    @Test
    void gameFinishesAfterTheLastRandomRound() {
        GameRoom room = setUpTwoPlayerRandomRoom(2);
        Long hostParticipantId = gridBattleOnlineService.getState(room, HOST).getYourParticipantId();
        Long guestParticipantId = gridBattleOnlineService.getState(room, GUEST).getYourParticipantId();

        // Round 0 (index 0): host's turn to pick and play.
        GridBattleStateDto round1 = gridBattleOnlineService.getState(room, HOST);
        assertThat(round1.getPickerParticipantId()).isEqualTo(hostParticipantId);
        Long g1 = round1.getGridChoices().get(0).getId();
        gridBattleOnlineService.chooseGrid(room, HOST, g1);
        Long a1 = gridRepository.findById(g1).orElseThrow().getEntries().iterator().next().getAthlete().getId();
        gridBattleOnlineService.guess(room, HOST, a1);
        GridBattleStateDto round2 = gridBattleOnlineService.advanceToNextGrid(room, HOST);

        // Round 1 (index 1): guest's turn to pick and play.
        assertThat(round2.isAwaitingGridChoice()).isTrue();
        assertThat(round2.getPickerParticipantId()).isEqualTo(guestParticipantId);
        Long g2 = round2.getGridChoices().get(0).getId();
        gridBattleOnlineService.chooseGrid(room, GUEST, g2);
        Long a2 = gridRepository.findById(g2).orElseThrow().getEntries().iterator().next().getAthlete().getId();
        gridBattleOnlineService.guess(room, GUEST, a2);

        GridBattleStateDto finalState = gridBattleOnlineService.advanceToNextGrid(room, HOST);
        assertThat(finalState.isFinished()).isTrue();
    }
}
