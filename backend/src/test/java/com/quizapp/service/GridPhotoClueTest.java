package com.quizapp.service;

import com.quizapp.dto.GridEntryViewDto;
import com.quizapp.model.Athlete;
import com.quizapp.model.Club;
import com.quizapp.model.Grid;
import com.quizapp.model.GridEntry;
import com.quizapp.repository.AthleteRepository;
import com.quizapp.repository.ClubRepository;
import com.quizapp.repository.GridRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

// On a grid without ranking, a tile's photo is the clue - unless the tile shows a logo
// (an "Oscars" badge on a best-picture grid). Then the logo is the clue, and the photo -
// a poster with the answer printed on it - stays hidden until the entry is found.
@SpringBootTest
class GridPhotoClueTest {

    @Autowired private GridPlayService gridPlayService;
    @Autowired private GridRepository gridRepository;
    @Autowired private AthleteRepository athleteRepository;
    @Autowired private ClubRepository clubRepository;

    private GridEntry entry(String name, Club club, int order) {
        Athlete a = new Athlete();
        a.setName(name + " " + System.nanoTime());
        a.setSport("Movies");
        a.setPhotoUrl("https://example.com/" + name + ".jpg");
        a = athleteRepository.save(a);
        GridEntry e = new GridEntry();
        e.setAthlete(a);
        e.setClub(club);
        e.setOrderIndex(order);
        return e;
    }

    @Test
    void aVisibleLogoKeepsThePosterHiddenUntilFound() {
        Club oscars = new Club();
        oscars.setName("Oscars " + System.nanoTime());
        oscars.setSport("Movies");
        oscars.setLogoUrl("https://example.com/oscars.png");
        oscars = clubRepository.save(oscars);

        GridEntry withLogo = entry("Hamnet", oscars, 0);
        GridEntry logoHidden = entry("Sinners", oscars, 1);
        logoHidden.setShowLogo(false);
        GridEntry noClub = entry("Frankenstein", null, 2);

        Grid g = new Grid();
        g.setTitle("Best picture " + System.nanoTime());
        g.setSport("Movies");
        g.setWeekStartDate(LocalDate.now());
        g.setMaxStrikes(3);
        g.setRanked(false);
        Set<GridEntry> entries = new HashSet<>(Set.of(withLogo, logoHidden, noClub));
        for (GridEntry e : entries) e.setGrid(g);
        g.setEntries(entries);
        g = gridRepository.save(g);

        List<GridEntryViewDto> tiles = gridPlayService.getMultiplayerStartState(g.getId()).getEntries();

        assertThat(tiles.stream().filter(e -> e.getLogoUrl() != null))
                .as("the logo is the clue, the poster waits")
                .singleElement()
                .satisfies(e -> assertThat(e.getAthletePhotoUrl()).isNull());
        assertThat(tiles.stream().filter(e -> e.getLogoUrl() == null))
                .as("no logo shown (hidden, or no club at all): the photo is the clue, as before")
                .hasSize(2)
                .allSatisfy(e -> assertThat(e.getAthletePhotoUrl()).isNotNull());
    }
}
