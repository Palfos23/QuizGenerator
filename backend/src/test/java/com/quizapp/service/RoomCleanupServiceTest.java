package com.quizapp.service;

import com.quizapp.dto.BullseyeEntryInputDto;
import com.quizapp.dto.BullseyeQuestionRequest;
import com.quizapp.dto.FlashbackYearRequest;
import com.quizapp.model.Athlete;
import com.quizapp.model.GameRoom;
import com.quizapp.model.ImposterGrid;
import com.quizapp.model.ImposterTile;
import com.quizapp.model.RoomGameType;
import com.quizapp.repository.AthleteRepository;
import com.quizapp.repository.GameRoomRepository;
import com.quizapp.repository.ImposterGridRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

// Regression coverage for a real production crash: RoomCleanupService's
// deleteRoomData() had no branch at all for Bullseye/Flashback, and its
// Imposter branch deleted only the room state, not the ImposterParticipantState/
// ImposterFlippedTile rows that reference each GameRoomParticipant. Deleting the
// GameRoom (which cascades to its participants) with those child rows still
// pointing at the about-to-vanish participants threw
// org.hibernate.exception.ConstraintViolationException on the game_room_participants
// FK - confirmed via the actual reported log line - and because the whole hourly
// sweep ran as one transaction, that one broken room wedged cleanup for every
// other stale room too, every hour, until fixed. These tests play each
// previously-broken game far enough to create real participant-state rows, then
// call the same deleteRoomById() the sweep now uses per room, and assert it
// doesn't throw and the room is actually gone.
@SpringBootTest
class RoomCleanupServiceTest {

    @Autowired
    private RoomService roomService;
    @Autowired
    private RoomCleanupService roomCleanupService;
    @Autowired
    private GameRoomRepository gameRoomRepository;
    @Autowired
    private AthleteRepository athleteRepository;
    @Autowired
    private ImposterGridRepository imposterGridRepository;
    @Autowired
    private ImposterOnlineService imposterOnlineService;
    @Autowired
    private BullseyeOnlineService bullseyeOnlineService;
    @Autowired
    private BullseyeAdminService bullseyeAdminService;
    @Autowired
    private FlashbackOnlineService flashbackOnlineService;
    @Autowired
    private FlashbackAdminService flashbackAdminService;

    private static final String HOST = "cleanup-host@example.com";
    private static final String GUEST = "cleanup-guest@example.com";

    @Test
    void deletesAFlashbackRoomWithParticipantStateWithoutThrowing() {
        for (int i = 0; i < 3; i++) {
            FlashbackYearRequest request = new FlashbackYearRequest();
            request.setTitle("Cleanup test year " + System.nanoTime());
            request.setYear(1990 + i);
            request.setHints(List.of("hint one", "hint two"));
            flashbackAdminService.create(request);
        }

        GameRoom room = roomService.createRoomShell(RoomGameType.FLASHBACK, HOST, "Host", null);
        flashbackOnlineService.initializeYearSequence(room, 2);
        roomService.join(room.getRoomCode(), GUEST, "Guest", null);
        room = roomService.findByCode(room.getRoomCode());
        flashbackOnlineService.startGame(room, HOST);

        Long roomId = room.getId();
        assertThatCode(() -> roomCleanupService.deleteRoomById(roomId)).doesNotThrowAnyException();
        assertThat(gameRoomRepository.findById(roomId)).isEmpty();
    }

    @Test
    void deletesABullseyeRoomWithParticipantStateWithoutThrowing() {
        Athlete a = athleteRepository.save(newAthlete("Cleanup Athlete A " + System.nanoTime()));
        Athlete b = athleteRepository.save(newAthlete("Cleanup Athlete B " + System.nanoTime()));
        BullseyeQuestionRequest request = new BullseyeQuestionRequest();
        request.setTitle("Cleanup test question " + System.nanoTime());
        request.setSport("Football");
        request.setTargetValue(10);
        request.setStatLabel("goals");
        request.setEntries(List.of(entry(a.getId(), 8), entry(b.getId(), 12)));
        bullseyeAdminService.create(request);

        GameRoom room = roomService.createRoomShell(RoomGameType.BULLSEYE, HOST, "Host", null);
        roomService.join(room.getRoomCode(), GUEST, "Guest", null);
        room = roomService.findByCode(room.getRoomCode());
        bullseyeOnlineService.startGame(room, HOST);

        Long roomId = room.getId();
        assertThatCode(() -> roomCleanupService.deleteRoomById(roomId)).doesNotThrowAnyException();
        assertThat(gameRoomRepository.findById(roomId)).isEmpty();
    }

    @Test
    void deletesAnImposterRoomWithParticipantStateWithoutThrowing() {
        for (int i = 0; i < 6; i++) {
            saveGridWithOneTile("Cleanup Board " + System.nanoTime() + "-" + i);
        }

        GameRoom room = roomService.createRoomShell(RoomGameType.IMPOSTER, HOST, "Host", null);
        imposterOnlineService.initializeImposterSequence(room, null, 2);
        roomService.join(room.getRoomCode(), GUEST, "Guest", null);
        room = roomService.findByCode(room.getRoomCode());
        imposterOnlineService.startGame(room, HOST);

        Long roomId = room.getId();
        assertThatCode(() -> roomCleanupService.deleteRoomById(roomId)).doesNotThrowAnyException();
        assertThat(gameRoomRepository.findById(roomId)).isEmpty();
    }

    private Athlete newAthlete(String name) {
        Athlete a = new Athlete();
        a.setName(name);
        a.setSport("Football");
        return a;
    }

    private BullseyeEntryInputDto entry(Long athleteId, Integer statValue) {
        BullseyeEntryInputDto dto = new BullseyeEntryInputDto();
        dto.setAthleteId(athleteId);
        dto.setStatValue(statValue);
        return dto;
    }

    private ImposterGrid saveGridWithOneTile(String title) {
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
}
