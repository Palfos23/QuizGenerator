package com.quizapp.service;

import com.quizapp.dto.BullseyeEntryInputDto;
import com.quizapp.dto.BullseyeQuestionAdminDetailDto;
import com.quizapp.dto.BullseyeQuestionRequest;
import com.quizapp.model.Athlete;
import com.quizapp.repository.AthleteRepository;
import com.quizapp.repository.BullseyeQuestionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class BullseyeAdminServiceTest {

    @Autowired
    private BullseyeAdminService bullseyeAdminService;
    @Autowired
    private AthleteRepository athleteRepository;
    @Autowired
    private BullseyeQuestionRepository bullseyeQuestionRepository;

    private Athlete haaland;
    private Athlete salah;

    @BeforeEach
    void setUp() {
        haaland = athleteRepository.save(newAthlete("Erling Haaland " + System.nanoTime()));
        salah = athleteRepository.save(newAthlete("Mohamed Salah " + System.nanoTime()));
    }

    private Athlete newAthlete(String name) {
        Athlete a = new Athlete();
        a.setName(name);
        a.setSport("Football");
        return a;
    }

    private BullseyeQuestionRequest baseRequest() {
        BullseyeQuestionRequest request = new BullseyeQuestionRequest();
        request.setTitle("Test question " + System.nanoTime());
        request.setSport("Football");
        request.setTargetValue(13);
        request.setStatLabel("goals in the Premier League 2024/25");
        request.setEntries(List.of(
                entry(haaland.getId(), 27),
                entry(salah.getId(), 18)
        ));
        return request;
    }

    private BullseyeEntryInputDto entry(Long athleteId, Integer statValue) {
        BullseyeEntryInputDto dto = new BullseyeEntryInputDto();
        dto.setAthleteId(athleteId);
        dto.setStatValue(statValue);
        return dto;
    }

    @Test
    void createPersistsEntriesWithCorrectStatValues() {
        BullseyeQuestionAdminDetailDto detail = bullseyeAdminService.create(baseRequest());

        assertThat(detail.getTargetValue()).isEqualTo(13);
        assertThat(detail.getEntries()).hasSize(2);
        assertThat(detail.getEntries())
                .anyMatch(e -> e.getAthlete().getId().equals(haaland.getId()) && e.getStatValue().equals(27))
                .anyMatch(e -> e.getAthlete().getId().equals(salah.getId()) && e.getStatValue().equals(18));
    }

    @Test
    void updatePreservesEntryIdsByAthlete() {
        BullseyeQuestionAdminDetailDto created = bullseyeAdminService.create(baseRequest());
        Long haalandEntryIdBefore = created.getEntries().stream()
                .filter(e -> e.getAthlete().getId().equals(haaland.getId()))
                .findFirst().orElseThrow().getId();

        BullseyeQuestionRequest update = baseRequest();
        update.setEntries(List.of(entry(haaland.getId(), 30), entry(salah.getId(), 18)));
        BullseyeQuestionAdminDetailDto updated = bullseyeAdminService.update(created.getId(), update);

        Long haalandEntryIdAfter = updated.getEntries().stream()
                .filter(e -> e.getAthlete().getId().equals(haaland.getId()))
                .findFirst().orElseThrow().getId();
        assertThat(haalandEntryIdAfter).isEqualTo(haalandEntryIdBefore);
        assertThat(updated.getEntries()).anyMatch(e -> e.getAthlete().getId().equals(haaland.getId()) && e.getStatValue().equals(30));
    }

    @Test
    void createRejectsFewerThanTwoEntries() {
        BullseyeQuestionRequest request = baseRequest();
        request.setEntries(List.of(entry(haaland.getId(), 27)));

        assertThatThrownBy(() -> bullseyeAdminService.create(request))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createRejectsAnEntryWithNoStatValue() {
        BullseyeQuestionRequest request = baseRequest();
        request.setEntries(List.of(entry(haaland.getId(), 27), entry(salah.getId(), null)));

        assertThatThrownBy(() -> bullseyeAdminService.create(request))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void persistsTheEntireCategoryPoolFlag() {
        BullseyeQuestionRequest request = baseRequest();
        request.setEntireCategoryPool(true);

        BullseyeQuestionAdminDetailDto detail = bullseyeAdminService.create(request);

        assertThat(detail.isEntireCategoryPool()).isTrue();
    }

    @Test
    void deleteCascadesToEntries() {
        BullseyeQuestionAdminDetailDto created = bullseyeAdminService.create(baseRequest());
        bullseyeAdminService.delete(created.getId());

        assertThat(bullseyeQuestionRepository.findById(created.getId())).isEmpty();
    }
}
