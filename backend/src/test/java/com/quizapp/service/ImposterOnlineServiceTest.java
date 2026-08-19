package com.quizapp.service;

import com.quizapp.dto.ImposterOnlineStateDto;
import com.quizapp.model.Athlete;
import com.quizapp.model.GameRoom;
import com.quizapp.model.ImposterGrid;
import com.quizapp.model.ImposterTile;
import com.quizapp.model.RoomGameType;
import com.quizapp.repository.AthleteRepository;
import com.quizapp.repository.ImposterGridRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Mirrors GridBattleOnlineServiceTest exactly - exercises "Random" Imposter
// Battle's new round-choice picker end to end through the service layer.
@SpringBootTest
class ImposterOnlineServiceTest {

    @Autowired
    private RoomService roomService;
    @Autowired
    private ImposterOnlineService imposterOnlineService;
    @Autowired
    private ImposterGridRepository imposterGridRepository;
    @Autowired
    private AthleteRepository athleteRepository;

    private static final String HOST = "host@example.com";
    private static final String GUEST = "guest@example.com";

    @BeforeEach
    void seedEnoughBoardsToChooseFrom() {
        for (int i = 0; i < 6; i++) {
            saveGridWithOneFitTile("Battle Board " + System.nanoTime() + "-" + i);
        }
    }

    private ImposterGrid saveGridWithOneFitTile(String title) {
        Athlete a = new Athlete();
        a.setName("Player " + System.nanoTime());
        a.setSport("Football");
        a = athleteRepository.save(a);

        ImposterGrid g = new ImposterGrid();
        g.setTitle(title);
        g.setSport("Football");

        ImposterTile t = new ImposterTile();
        t.setAthlete(a);
        t.setImposter(false);
        t.setOrderIndex(0);
        List<ImposterTile> tiles = new ArrayList<>();
        tiles.add(t);
        g.setTiles(tiles);

        return imposterGridRepository.save(g);
    }

    private GameRoom setUpTwoPlayerRandomRoom(int rounds) {
        GameRoom room = roomService.createRoomShell(RoomGameType.IMPOSTER, HOST, "Host", null);
        imposterOnlineService.initializeImposterSequence(room, null, rounds);
        roomService.join(room.getRoomCode(), GUEST, "Guest", null);
        room = roomService.findByCode(room.getRoomCode());
        imposterOnlineService.startGame(room, HOST);
        return room;
    }

    @Test
    void randomRoomOffersThreeChoicesToTheFirstPicker() {
        GameRoom room = setUpTwoPlayerRandomRoom(2);
        ImposterOnlineStateDto state = imposterOnlineService.getState(room, HOST);

        assertThat(state.isAwaitingGridChoice()).isTrue();
        assertThat(state.getGridChoices()).hasSize(3);
        assertThat(state.getTotalGrids()).isEqualTo(2);
        assertThat(state.getCurrentGridIndex()).isZero();
        Long hostParticipantId = state.getYourParticipantId();
        assertThat(state.getPickerParticipantId()).isEqualTo(hostParticipantId);
    }

    @Test
    void repeatedPollsOfferTheSameThreeChoicesUntilPicked() {
        GameRoom room = setUpTwoPlayerRandomRoom(2);
        ImposterOnlineStateDto first = imposterOnlineService.getState(room, HOST);
        ImposterOnlineStateDto second = imposterOnlineService.getState(room, HOST);

        List<Long> firstIds = first.getGridChoices().stream().map(g -> g.getId()).sorted().toList();
        List<Long> secondIds = second.getGridChoices().stream().map(g -> g.getId()).sorted().toList();
        assertThat(secondIds).isEqualTo(firstIds);
    }

    @Test
    void onlyThePickerCanChooseAndOnlyFromTheOfferedOptions() {
        GameRoom room = setUpTwoPlayerRandomRoom(2);
        ImposterOnlineStateDto state = imposterOnlineService.getState(room, HOST);
        Long offeredId = state.getGridChoices().get(0).getId();

        assertThatThrownBy(() -> imposterOnlineService.chooseGrid(room, GUEST, offeredId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not your turn");

        ImposterGrid notOffered = imposterGridRepository.findAll().stream()
                .filter(g -> state.getGridChoices().stream().noneMatch(c -> c.getId().equals(g.getId())))
                .findFirst().orElseThrow();
        assertThatThrownBy(() -> imposterOnlineService.chooseGrid(room, HOST, notOffered.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("offered");
    }

    @Test
    void choosingABoardStartsTheRoundAndFinishingItOffersAFreshChoiceExcludingThePrevious() {
        GameRoom room = setUpTwoPlayerRandomRoom(2);
        ImposterOnlineStateDto beforePick = imposterOnlineService.getState(room, HOST);
        Long firstGridId = beforePick.getGridChoices().get(0).getId();

        ImposterOnlineStateDto afterPick = imposterOnlineService.chooseGrid(room, HOST, firstGridId);
        assertThat(afterPick.isAwaitingGridChoice()).isFalse();
        assertThat(afterPick.getCurrentGridId()).isEqualTo(firstGridId);
        assertThat(afterPick.getTiles()).hasSize(1);

        Long tileId = imposterGridRepository.findById(firstGridId).orElseThrow()
                .getTiles().iterator().next().getId();
        ImposterOnlineStateDto afterFlip = imposterOnlineService.flip(room, HOST, tileId);
        assertThat(afterFlip.isBoardComplete()).isTrue();

        ImposterOnlineStateDto round2 = imposterOnlineService.advanceToNextBoard(room, HOST);
        assertThat(round2.isFinished()).isFalse();
        assertThat(round2.getCurrentGridIndex()).isEqualTo(1);
        assertThat(round2.isAwaitingGridChoice()).isTrue();
        assertThat(round2.getGridChoices()).extracting(g -> g.getId()).doesNotContain(firstGridId);
        Long guestParticipantId = imposterOnlineService.getState(room, GUEST).getYourParticipantId();
        assertThat(round2.getPickerParticipantId()).isEqualTo(guestParticipantId);
    }

    @Test
    void gameFinishesAfterTheLastRandomRound() {
        GameRoom room = setUpTwoPlayerRandomRoom(2);
        Long hostParticipantId = imposterOnlineService.getState(room, HOST).getYourParticipantId();
        Long guestParticipantId = imposterOnlineService.getState(room, GUEST).getYourParticipantId();

        ImposterOnlineStateDto round1 = imposterOnlineService.getState(room, HOST);
        assertThat(round1.getPickerParticipantId()).isEqualTo(hostParticipantId);
        Long g1 = round1.getGridChoices().get(0).getId();
        imposterOnlineService.chooseGrid(room, HOST, g1);
        Long t1 = imposterGridRepository.findById(g1).orElseThrow().getTiles().iterator().next().getId();
        imposterOnlineService.flip(room, HOST, t1);
        ImposterOnlineStateDto round2 = imposterOnlineService.advanceToNextBoard(room, HOST);

        assertThat(round2.isAwaitingGridChoice()).isTrue();
        assertThat(round2.getPickerParticipantId()).isEqualTo(guestParticipantId);
        Long g2 = round2.getGridChoices().get(0).getId();
        imposterOnlineService.chooseGrid(room, GUEST, g2);
        Long t2 = imposterGridRepository.findById(g2).orElseThrow().getTiles().iterator().next().getId();
        imposterOnlineService.flip(room, GUEST, t2);

        ImposterOnlineStateDto finalState = imposterOnlineService.advanceToNextBoard(room, HOST);
        assertThat(finalState.isFinished()).isTrue();
    }
}
